package com.guilhermedev.librarymedia.library;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.common.PageResponse;
import com.guilhermedev.librarymedia.list.ListItem;
import com.guilhermedev.librarymedia.list.ListItemRepository;
import com.guilhermedev.librarymedia.list.MediaList;
import com.guilhermedev.librarymedia.list.MediaListRepository;
import com.guilhermedev.librarymedia.media.*;
import com.guilhermedev.librarymedia.user.UserAccount;
import com.guilhermedev.librarymedia.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/library")
@Tag(name = "Library", description = "Manage personal media entries, statuses, and ratings")
public class LibraryController {
    private final UserMediaRepository userMediaRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final MediaListRepository listRepository;
    private final ListItemRepository itemRepository;
    private final List<MediaProvider> providers;

    public LibraryController(UserMediaRepository userMediaRepository, UserRepository userRepository,
                             MediaRepository mediaRepository, MediaListRepository listRepository,
                             ListItemRepository itemRepository, List<MediaProvider> providers) {
        this.userMediaRepository = userMediaRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.providers = providers;
    }

    @GetMapping
    @Transactional(readOnly = true)
    // The endpoint accepts only a small set of sort fields so callers cannot sort by internal columns.
    public PageResponse<LibraryEntryView> getLibrary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) ConsumptionStatus status,
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Sort requestedSort = parseSort(sort);
        PageRequest pageRequest = pageRequest(page, size, requestedSort);
        Specification<UserMedia> criteria = (root, query, builder) ->
                builder.equal(root.get("user").get("id"), CurrentUser.id());
        if (type != null) {
            criteria = criteria.and((root, query, builder) ->
                    builder.equal(root.get("media").get("type"), type));
        }
        if (status != null) {
            criteria = criteria.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (listId != null) {
            if (listId < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "listId must be positive");
            }
            listRepository.findByIdAndUserId(listId, CurrentUser.id())
                    .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "List not found"));
            criteria = criteria.and((root, query, builder) -> {
                var membership = query.subquery(Long.class);
                var item = membership.from(ListItem.class);
                membership.select(item.get("userMedia").get("id"))
                        .where(builder.equal(item.get("list").get("id"), listId));
                return root.get("id").in(membership);
            });
        }
        if (q != null && !q.isBlank()) {
            if (q.trim().length() > 200) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Search text is too long");
            }
            String titleQuery = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            criteria = criteria.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("media").get("title")), titleQuery));
        }
        Page<UserMedia> entries = userMediaRepository.findAll(criteria, pageRequest);
        return PageResponse.from(entries.map(entry -> LibraryEntryView.from(entry, itemRepository)));
    }

    @PostMapping
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<AddToLibraryResponse> addToLibrary(@Valid @RequestBody AddToLibraryRequest request) {
        validateInitialState(request);
        Long userId = CurrentUser.id();
        MediaProvider provider = providers.stream()
                .filter(candidate -> candidate.providerId().equalsIgnoreCase(request.provider()))
                .findFirst()
                .orElseThrow(() -> notFound("MEDIA_NOT_FOUND", "Media not found"));
        MediaRecord metadata;
        try {
            metadata = findMetadata(provider, request.externalId(), request.type());
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_PROVIDER_UNAVAILABLE",
                    "The media provider is temporarily unavailable");
        }
        if (metadata == null) throw notFound("MEDIA_NOT_FOUND", "Media not found");
        MediaType type = MediaType.valueOf(metadata.type());
        String canonicalProvider = metadata.provider().toLowerCase(Locale.ROOT);

        MediaEntity media = mediaRepository.findByProviderAndExternalId(canonicalProvider, metadata.externalId())
                .orElseGet(() -> mediaRepository.save(new MediaEntity(type, canonicalProvider,
                        metadata.externalId(), requiredTitle(metadata.title()), metadata.releaseYear(),
                        metadata.genre(), metadata.synopsis(), metadata.coverUrl(), metadata.externalRating())));

        List<MediaList> lists = new ArrayList<>();
        // Sort locks so concurrent requests targeting the same lists acquire them in the same order.
        for (Long listId : new LinkedHashSet<>(request.listIds()).stream().sorted().toList()) {
            lists.add(listRepository.findLockedByIdAndUserId(listId, userId)
                    .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "List not found")));
        }

        UserMedia entry = userMediaRepository.findLockedByUserIdAndMediaId(userId, media.getId()).orElse(null);
        boolean alreadyInLibrary = entry != null;
        if (entry == null) {
            UserAccount user = userRepository.findById(userId)
                    .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "User not found"));
            ConsumptionStatus initialStatus = request.consumed()
                    ? ConsumptionStatus.DONE : ConsumptionStatus.WANT;
            entry = userMediaRepository.save(new UserMedia(user, media, initialStatus, request.rating()));
        }

        List<Long> addedToLists = new ArrayList<>();
        List<Long> alreadyInLists = new ArrayList<>();
        for (MediaList list : lists) {
            if (itemRepository.existsByListIdAndUserMediaId(list.getId(), entry.getId())) {
                alreadyInLists.add(list.getId());
            } else {
                itemRepository.save(new ListItem(list, entry));
                addedToLists.add(list.getId());
            }
        }
        AddToLibraryResponse response = new AddToLibraryResponse(entry.getId(), alreadyInLibrary, addedToLists,
                alreadyInLists, entry.getStatus().name(), entry.getRating());
        return ResponseEntity.status(alreadyInLibrary ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{entryId}")
    @Transactional(readOnly = true)
    public LibraryEntryView getEntry(@PathVariable Long entryId) {
        return LibraryEntryView.from(ownedEntry(entryId), itemRepository);
    }

    @DeleteMapping("/{entryId}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Void> removeFromLibrary(@PathVariable Long entryId,
                                                   @RequestParam(defaultValue = "false") boolean confirm) {
        UserMedia existing = ownedEntry(entryId);
        List<ListItem> memberships = itemRepository.findAllByUserMediaId(existing.getId());
        if (memberships.stream().anyMatch(item ->
                !item.getList().getUser().getId().equals(CurrentUser.id()) ||
                        !item.getUserMedia().getUser().getId().equals(CurrentUser.id()))) {
            throw notFound("RESOURCE_NOT_FOUND", "Library entry not found");
        }
        // Match list-to-entry lock order used by add/remove membership requests.
        memberships.stream().map(item -> item.getList().getId()).distinct().sorted()
                .forEach(listId -> listRepository.findLockedByIdAndUserId(listId, CurrentUser.id())
                        .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "List not found")));
        UserMedia entry = ownedEntryForUpdate(entryId);
        if (!confirm) {
            throw new ApiException(HttpStatus.CONFLICT, "LIBRARY_REMOVAL_CONFIRMATION_REQUIRED",
                    "Confirm to remove this media and its rating from your library");
        }
        itemRepository.deleteAllByUserMediaId(entry.getId());
        userMediaRepository.delete(entry);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{entryId}/status")
    @Transactional
    public LibraryEntryView updateStatus(@PathVariable Long entryId, @Valid @RequestBody StatusRequest request) {
        UserMedia entry = ownedEntryForUpdate(entryId);
        entry.changeStatus(request.status());
        return LibraryEntryView.from(entry, itemRepository);
    }

    @PutMapping("/{entryId}/rating")
    @Transactional
    public LibraryEntryView updateRating(@PathVariable Long entryId, @Valid @RequestBody RatingRequest request) {
        UserMedia entry = ownedEntryForUpdate(entryId);
        if (entry.getStatus() != ConsumptionStatus.DONE) {
            throw new ApiException(HttpStatus.CONFLICT, "RATING_REQUIRES_COMPLETED",
                    "A rating can only be set or changed for completed media");
        }
        entry.rate(request.rating());
        return LibraryEntryView.from(entry, itemRepository);
    }

    private MediaRecord findMetadata(MediaProvider provider, String externalId, MediaType requestedType) {
        if (requestedType != null) return provider.findByExternalId(externalId, requestedType);
        for (MediaType type : MediaType.values()) {
            if (!provider.supports(type)) continue;
            MediaRecord record = provider.findByExternalId(externalId, type);
            if (record != null) return record;
        }
        return null;
    }

    private void validateInitialState(AddToLibraryRequest request) {
        if (request.consumed() && request.rating() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RATING_REQUIRED",
                    "A rating is required when media is already completed");
        }
        if (!request.consumed() && request.rating() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RATING_REQUIRES_COMPLETED",
                    "A rating can only be set for completed media");
        }
        if (request.rating() != null && (request.rating() < 1 || request.rating() > 10)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Rating must be between 1 and 10");
        }
    }

    private UserMedia ownedEntry(Long entryId) {
        return userMediaRepository.findByIdAndUserId(entryId, CurrentUser.id())
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "Library entry not found"));
    }

    private UserMedia ownedEntryForUpdate(Long entryId) {
        return userMediaRepository.findLockedByIdAndUserId(entryId, CurrentUser.id())
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "Library entry not found"));
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Page must be zero or greater and size must be between 1 and 50");
        }
        return PageRequest.of(page, size, sort);
    }

    private static Sort parseSort(String sortValue) {
        if (sortValue == null || sortValue.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Sort is required");
        }
        String[] parts = sortValue.split(",", -1);
        String property = switch (parts[0].trim()) {
            case "title" -> "media.title";
            case "rating" -> "rating";
            case "releaseYear" -> "media.releaseYear";
            case "createdAt" -> "createdAt";
            case "status" -> "status";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Unsupported sort field");
        };
        if (parts.length > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid sort direction");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid sort direction");
        }
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static String requiredTitle(String title) {
        return title == null || title.isBlank() ? "Untitled" : title;
    }

    private static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public record AddToLibraryRequest(
            @NotBlank String provider,
            @NotBlank @Size(max = 100) String externalId,
            @NotEmpty @Size(max = 50) List<@NotNull Long> listIds,
            @NotNull Boolean consumed,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10) Integer rating,
            MediaType type) { }
    public record StatusRequest(@NotNull ConsumptionStatus status) { }
    public record RatingRequest(@NotNull @jakarta.validation.constraints.Min(1)
                                @jakarta.validation.constraints.Max(10) Integer rating) { }
    public record AddToLibraryResponse(Long entryId, boolean alreadyInLibrary, List<Long> addedToLists,
                                       List<Long> alreadyInLists, String status, Integer rating) { }
    public record ListReference(Long id, String name) { }
    public record LibraryEntryView(Long entryId, Long mediaId, String provider, String externalId, String type,
                                   String title, Integer releaseYear, String genre, String synopsis, String coverUrl,
                                   java.math.BigDecimal externalRating, String status, String statusLabel,
                                   Integer rating, List<ListReference> lists) {
        static LibraryEntryView from(UserMedia entry, ListItemRepository itemRepository) {
            MediaEntity media = entry.getMedia();
            List<ListReference> lists = itemRepository.findAllByUserMediaId(entry.getId()).stream()
                    .map(item -> new ListReference(item.getList().getId(), item.getList().getName()))
                    .toList();
            return new LibraryEntryView(entry.getId(), media.getId(), media.getProvider(), media.getExternalId(),
                    media.getType().name(), media.getTitle(), media.getReleaseYear(), media.getGenre(),
                    media.getSynopsis(), media.getCoverUrl(), media.getExternalRating(),
                    entry.getStatus().name(), entry.getStatus().labelFor(media.getType()), entry.getRating(), lists);
        }
    }
}
