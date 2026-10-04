package com.guilhermedev.librarymedia.list;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.library.ConsumptionStatus;
import com.guilhermedev.librarymedia.library.UserMedia;
import com.guilhermedev.librarymedia.library.UserMediaRepository;
import com.guilhermedev.librarymedia.media.MediaEntity;
import com.guilhermedev.librarymedia.media.MediaType;
import com.guilhermedev.librarymedia.sharing.ListShareRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lists")
@Tag(name = "Lists", description = "Create and organize personal media lists")
public class ListController {
    private final MediaListRepository listRepository;
    private final ListItemRepository itemRepository;
    private final UserMediaRepository userMediaRepository;
    private final ListShareRepository shareRepository;

    public ListController(MediaListRepository listRepository, ListItemRepository itemRepository,
                          UserMediaRepository userMediaRepository, ListShareRepository shareRepository) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.userMediaRepository = userMediaRepository;
        this.shareRepository = shareRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ListSummary> getLists() {
        Long userId = CurrentUser.id();
        return listRepository.findAllByUserIdOrderByFavoritesDescNameAsc(userId).stream()
                .map(this::summary).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ListSummary createList(@Valid @RequestBody CreateListRequest request) {
        Long userId = CurrentUser.id();
        ensureUniqueName(userId, request.name(), null);
        MediaList list = listRepository.save(new MediaList(
                listRepository.findByUserIdAndFavoritesTrue(userId)
                        .map(MediaList::getUser)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User not found")),
                request.name().trim(), false));
        return summary(list);
    }

    @GetMapping("/{listId}")
    @Transactional(readOnly = true)
    public ListSummary getList(@PathVariable Long listId) {
        return summary(ownedList(listId, CurrentUser.id()));
    }

    @PatchMapping("/{listId}")
    @Transactional
    public ListSummary renameList(@PathVariable Long listId, @Valid @RequestBody RenameListRequest request) {
        MediaList list = ownedList(listId, CurrentUser.id());
        if (list.isFavorites()) {
            throw new ApiException(HttpStatus.CONFLICT, "FAVORITES_LIST_PROTECTED",
                    "The Favorites list cannot be renamed");
        }
        ensureUniqueName(CurrentUser.id(), request.name(), listId);
        list.rename(request.name().trim());
        return summary(list);
    }

    @DeleteMapping("/{listId}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Operation(summary = "Delete a list", description = "Deleting exclusive media requires confirm=true.")
    @ApiResponse(responseCode = "409", description = "Confirmation required for exclusive media",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Deleting this list removes media that is not in another list",
                              "code": "LIST_HAS_EXCLUSIVE_MEDIA",
                              "affectedMediaCount": 2
                            }
                            """)))
    public DeleteListResponse deleteList(@PathVariable Long listId,
                                         @RequestParam(defaultValue = "false") boolean confirm) {
        MediaList list = ownedListForUpdate(listId, CurrentUser.id());
        if (list.isFavorites()) {
            throw new ApiException(HttpStatus.CONFLICT, "FAVORITES_LIST_PROTECTED",
                    "The Favorites list cannot be deleted");
        }
        // Lock the list first, then its entries; membership mutations use the same order.
        userMediaRepository.lockAllByListIdAndUserId(listId, CurrentUser.id());
        List<Long> exclusiveEntries = itemRepository.findExclusiveEntryIds(listId, CurrentUser.id());
        if (!exclusiveEntries.isEmpty() && !confirm) {
            throw new ApiException(HttpStatus.CONFLICT, "LIST_HAS_EXCLUSIVE_MEDIA",
                    "Deleting this list removes media that is not in another list",
                    java.util.Map.of("affectedMediaCount", exclusiveEntries.size()));
        }
        for (Long entryId : exclusiveEntries) {
            itemRepository.deleteAllByUserMediaId(entryId);
            userMediaRepository.deleteById(entryId);
        }
        listRepository.delete(list);
        return new DeleteListResponse(true, exclusiveEntries.size());
    }

    @GetMapping("/{listId}/items")
    @Transactional(readOnly = true)
    public ListItemsResponse getItems(@PathVariable Long listId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) MediaType type,
                                      @RequestParam(required = false) ConsumptionStatus status) {
        ownedList(listId, CurrentUser.id());
        Page<ListItem> items = itemRepository.findFilteredByListId(
                listId, CurrentUser.id(), type, status, pageRequest(page, size));
        Page<ListItemView> views = items.map(item -> ListItemView.from(item.getUserMedia()));
        Map<String, String> links = views.isEmpty()
                ? Map.of("search", "/api/v1/media/search") : Map.of();
        return new ListItemsResponse(views.getContent(), views.getNumber(), views.getSize(),
                views.getTotalElements(), views.getTotalPages(), links);
    }

    @PostMapping("/{listId}/items")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<AddItemResponse> addItem(@PathVariable Long listId,
                                                   @Valid @RequestBody AddItemRequest request) {
        MediaList list = ownedListForUpdate(listId, CurrentUser.id());
        UserMedia entry = userMediaRepository.findLockedByIdAndUserId(request.entryId(), CurrentUser.id())
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "Library entry not found"));
        boolean alreadyInList = itemRepository.existsByListIdAndUserMediaId(listId, entry.getId());
        if (!alreadyInList) itemRepository.save(new ListItem(list, entry));
        AddItemResponse response = new AddItemResponse(entry.getId(), alreadyInList);
        return ResponseEntity.status(alreadyInList ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{listId}/items/{entryId}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RemoveItemResponse removeItem(@PathVariable Long listId, @PathVariable Long entryId,
                                         @RequestParam(defaultValue = "false") boolean confirm) {
        ownedListForUpdate(listId, CurrentUser.id());
        UserMedia entry = userMediaRepository.findLockedByIdAndUserId(entryId, CurrentUser.id())
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "Library entry not found"));
        if (!itemRepository.existsByListIdAndUserMediaId(listId, entryId)) {
            throw notFound("RESOURCE_NOT_FOUND", "Item was not found in this list");
        }
        long otherLists = itemRepository.countByUserMediaIdAndListIdNot(entryId, listId);
        if (otherLists == 0 && !confirm) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_LIST_CONFIRMATION_REQUIRED",
                    "Confirm to remove this media from your library");
        }
        itemRepository.deleteByListIdAndUserMediaId(listId, entryId);
        boolean removedFromLibrary = otherLists == 0;
        if (removedFromLibrary) userMediaRepository.delete(entry);
        return new RemoveItemResponse(entryId, removedFromLibrary);
    }

    private MediaList ownedList(Long listId, Long userId) {
        return listRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "List not found"));
    }

    private MediaList ownedListForUpdate(Long listId, Long userId) {
        return listRepository.findLockedByIdAndUserId(listId, userId)
                .orElseThrow(() -> notFound("RESOURCE_NOT_FOUND", "List not found"));
    }

    private void ensureUniqueName(Long userId, String name, Long currentListId) {
        boolean duplicate = listRepository.findAllByUserIdOrderByFavoritesDescNameAsc(userId).stream()
                .anyMatch(list -> !list.getId().equals(currentListId) &&
                        list.getName().equalsIgnoreCase(name.trim()));
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "LIST_NAME_ALREADY_EXISTS",
                    "A list with this name already exists");
        }
    }

    private ListSummary summary(MediaList list) {
        List<String> covers = itemRepository.findAllByListIdAndUserId(list.getId(), list.getUser().getId()).stream()
                .map(item -> item.getUserMedia().getMedia().getCoverUrl())
                .filter(url -> url != null && !url.isBlank()).distinct().limit(4).toList();
        boolean shared = shareRepository.findByListId(list.getId()).map(share ->
                share.isActive()).orElse(false);
        return new ListSummary(list.getId(), list.getName(), list.isFavorites(),
                itemRepository.countByListIdAndUserId(list.getId(), list.getUser().getId()), covers, shared);
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Page must be zero or greater and size must be between 1 and 50");
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "addedAt")
                .and(Sort.by(Sort.Direction.ASC, "userMedia.id"));
        return PageRequest.of(page, size, sort);
    }

    private static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public record CreateListRequest(@NotBlank @Size(max = 100) String name) { }
    public record RenameListRequest(@NotBlank @Size(max = 100) String name) { }
    public record AddItemRequest(@NotNull Long entryId) { }
    public record ListSummary(Long id, String name, boolean favorites, long itemCount,
                              List<String> coverUrls, boolean shared) { }
    public record DeleteListResponse(boolean deleted, int removedLibraryEntries) { }
    public record AddItemResponse(Long entryId, boolean alreadyInList) { }
    public record RemoveItemResponse(Long entryId, boolean removedFromLibrary) { }
    public record ListItemsResponse(List<ListItemView> content, int page, int size, long totalElements,
                                    int totalPages, Map<String, String> links) { }

    public record ListItemView(Long entryId, Long mediaId, String title, String type, Integer releaseYear,
                               String genre, String synopsis, String coverUrl, java.math.BigDecimal externalRating,
                               String status, String statusLabel, Integer rating) {
        static ListItemView from(UserMedia entry) {
            MediaEntity media = entry.getMedia();
            return new ListItemView(entry.getId(), media.getId(), media.getTitle(), media.getType().name(),
                    media.getReleaseYear(), media.getGenre(), media.getSynopsis(), media.getCoverUrl(),
                    media.getExternalRating(), entry.getStatus().name(),
                    entry.getStatus().labelFor(media.getType()), entry.getRating());
        }
    }
}
