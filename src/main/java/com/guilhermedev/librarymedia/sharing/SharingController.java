package com.guilhermedev.librarymedia.sharing;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.common.PageResponse;
import com.guilhermedev.librarymedia.list.ListItem;
import com.guilhermedev.librarymedia.list.ListItemRepository;
import com.guilhermedev.librarymedia.list.MediaList;
import com.guilhermedev.librarymedia.list.MediaListRepository;
import com.guilhermedev.librarymedia.media.MediaEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@RestController
@Tag(name = "Sharing", description = "Read-only public links for personal lists")
public class SharingController {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final MediaListRepository listRepository;
    private final ListItemRepository itemRepository;
    private final ListShareRepository shareRepository;
    private final String frontendBaseUrl;

    public SharingController(MediaListRepository listRepository, ListItemRepository itemRepository,
                             ListShareRepository shareRepository,
                             @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.shareRepository = shareRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/api/v1/lists/{listId}/share")
    @Transactional(readOnly = true)
    public ShareSettings getShareSettings(@PathVariable Long listId) {
        ownedList(listId);
        return shareRepository.findByListId(listId)
                .map(this::shareSettings)
                .orElseGet(() -> new ShareSettings(false, null, null));
    }

    @PutMapping("/api/v1/lists/{listId}/share")
    @Transactional
    public ShareSettings updateShareSettings(@PathVariable Long listId,
                                             @Valid @RequestBody ShareRequest request) {
        MediaList list = listRepository.findLockedByIdAndUserId(listId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "List not found"));
        ListShare share = shareRepository.findByListId(listId).orElse(null);
        if (request.active()) {
            if (share == null) share = shareRepository.save(new ListShare(list, newToken()));
            else share.setActive(true);
        } else if (share != null) {
            share.setActive(false);
        }
        return share == null ? new ShareSettings(false, null, null) : shareSettings(share);
    }

    @GetMapping("/api/v1/public/lists/{token}")
    @Transactional(readOnly = true)
    public PublicList getPublicList(@PathVariable String token,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = pageRequest(page, size);
        ListShare share = shareRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                        "Public list not found"));
        MediaList list = share.getList();
        Long ownerId = list.getUser().getId();
        Page<ListItem> entries = itemRepository.findOwnedByListId(list.getId(), ownerId, pageRequest);
        Page<PublicMedia> media = entries.map(item -> PublicMedia.from(item.getUserMedia().getMedia()));
        return new PublicList(list.getName(), itemRepository.countByListIdAndUserId(list.getId(), ownerId),
                PageResponse.from(media));
    }

    private MediaList ownedList(Long listId) {
        return listRepository.findByIdAndUserId(listId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "List not found"));
    }

    private ShareSettings shareSettings(ListShare share) {
        String url = frontendBaseUrl + "/shared/" + share.getToken();
        return new ShareSettings(share.isActive(), url, share.getToken());
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Page must be zero or greater and size must be between 1 and 50");
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "addedAt")
                .and(Sort.by(Sort.Direction.ASC, "userMedia.id"));
        return PageRequest.of(page, size, sort);
    }

    private static String newToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public record ShareRequest(@NotNull Boolean active) { }
    public record ShareSettings(boolean active, String url, String token) { }
    public record PublicList(String name, long mediaCount, PageResponse<PublicMedia> items) { }
    public record PublicMedia(String title, String type, Integer releaseYear, String genre, String synopsis,
                              String coverUrl, java.math.BigDecimal externalRating) {
        static PublicMedia from(MediaEntity media) {
            return new PublicMedia(media.getTitle(), media.getType().name(), media.getReleaseYear(),
                    media.getGenre(), media.getSynopsis(), media.getCoverUrl(), media.getExternalRating());
        }
    }
}
