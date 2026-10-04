package com.guilhermedev.librarymedia.media;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.library.UserMedia;
import com.guilhermedev.librarymedia.library.UserMediaRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Search and inspect books, movies, and series")
public class MediaController {
    private final MediaSearchService searchService;
    private final UserMediaRepository userMediaRepository;

    public MediaController(MediaSearchService searchService, UserMediaRepository userMediaRepository) {
        this.searchService = searchService;
        this.userMediaRepository = userMediaRepository;
    }

    @GetMapping("/search")
    public SearchResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) BigDecimal minRating) {
        validateSearch(q, page, size);
        if ((year != null && (year < 1 || year > 9999)) || (genre != null && genre.length() > 100)) {
            throw invalidSearch();
        }
        if (minRating != null && (minRating.compareTo(BigDecimal.ZERO) < 0 ||
                minRating.compareTo(BigDecimal.TEN) > 0)) {
            throw invalidSearch();
        }
        MediaSearchService.SearchResult result = searchService.search(q.trim(), page, size, type, year, genre,
                minRating);
        List<MediaSummary> content = result.content().stream().map(this::summary).toList();
        return new SearchResult(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/suggestions")
    public List<MediaSummary> suggestions(@RequestParam String q) {
        if (q == null || q.trim().length() < 2 || q.trim().length() > 100) throw invalidSearch();
        return searchService.suggestions(q.trim()).stream().map(this::summary).toList();
    }

    @GetMapping("/{provider}/{externalId}")
    public MediaSummary getByProvider(@PathVariable String provider, @PathVariable String externalId,
                                      @RequestParam(required = false) MediaType type) {
        return summary(searchService.findByExternalId(provider, externalId, type));
    }

    @GetMapping("/attribution")
    public Attribution attribution() {
        return new Attribution("This product uses the TMDB API but is not endorsed or certified by TMDB.",
                "https://www.themoviedb.org/");
    }

    private static void validateSearch(String query, int page, int size) {
        if (query == null || query.trim().length() < 2 || query.trim().length() > 100 ||
                page < 0 || size < 1 || size > 20) {
            throw invalidSearch();
        }
    }

    private static ApiException invalidSearch() {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Search requires a query, page >= 0, size between 1 and 20, and valid filters");
    }

    public record Attribution(String notice, String url) { }

    private MediaSummary summary(MediaRecord media) {
        UserMedia libraryEntry = userMediaRepository.findByUserIdAndProviderAndExternalId(
                CurrentUser.id(), media.provider().toLowerCase(java.util.Locale.ROOT), media.externalId())
                .orElse(null);
        return new MediaSummary(media.type(), media.provider(), media.externalId(), media.title(),
                media.releaseYear(), media.genre(), media.synopsis(), media.coverUrl(), media.externalRating(),
                libraryEntry != null, libraryEntry == null ? null : libraryEntry.getId());
    }

    public record MediaSummary(String type, String provider, String externalId, String title, Integer year,
                               String genre, String synopsis, String coverUrl,
                               BigDecimal externalRating, boolean inLibrary, Long entryId) { }

    public record SearchResult(List<MediaSummary> content, int page, int size,
                               long totalElements, int totalPages) { }
}
