package com.guilhermedev.librarymedia.ranking;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.common.PageResponse;
import com.guilhermedev.librarymedia.library.ConsumptionStatus;
import com.guilhermedev.librarymedia.library.UserMedia;
import com.guilhermedev.librarymedia.library.UserMediaRepository;
import com.guilhermedev.librarymedia.media.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/rankings")
@Tag(name = "Rankings", description = "Personal rankings of completed and rated media")
public class RankingController {
    private final UserMediaRepository userMediaRepository;

    public RankingController(UserMediaRepository userMediaRepository) {
        this.userMediaRepository = userMediaRepository;
    }

    @GetMapping("/{category}")
    @Transactional(readOnly = true)
    public PageResponse<RankingEntry> getRanking(@PathVariable String category,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Page must be zero or greater and size must be between 1 and 50");
        }
        MediaType type = toMediaType(category);
        Sort ordering = Sort.by(Sort.Direction.DESC, "rating")
                .and(Sort.by(Sort.Direction.ASC, "media.title"))
                .and(Sort.by(Sort.Direction.ASC, "id"));
        Page<UserMedia> entries = userMediaRepository.findAllByUserIdAndStatusAndRatingIsNotNullAndMediaType(
                CurrentUser.id(), ConsumptionStatus.DONE, type, PageRequest.of(page, size, ordering));
        List<RankingEntry> ranked = IntStream.range(0, entries.getContent().size())
                .mapToObj(index -> {
                    UserMedia entry = entries.getContent().get(index);
                    int position = (int) (entries.getNumber() * (long) entries.getSize()) + index + 1;
                    return new RankingEntry(position, entry.getId(),
                            new RankingMedia(entry.getMedia().getType().name(), entry.getMedia().getTitle(),
                                    entry.getMedia().getReleaseYear(), entry.getMedia().getCoverUrl()),
                            entry.getRating());
                })
                .toList();
        return new PageResponse<>(ranked, entries.getNumber(), entries.getSize(),
                entries.getTotalElements(), entries.getTotalPages());
    }

    private static MediaType toMediaType(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "books", "book" -> MediaType.BOOK;
            case "movies", "movie" -> MediaType.MOVIE;
            case "series" -> MediaType.SERIES;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Ranking category must be books, movies, or series");
        };
    }

    public record RankingEntry(int position, Long entryId, RankingMedia media, Integer rating) { }
    public record RankingMedia(String type, String title, Integer year, String coverUrl) { }
}
