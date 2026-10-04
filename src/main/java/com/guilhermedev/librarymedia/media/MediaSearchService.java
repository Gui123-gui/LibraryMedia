package com.guilhermedev.librarymedia.media;

import com.guilhermedev.librarymedia.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class MediaSearchService {
    private final List<MediaProvider> providers;

    public MediaSearchService(List<MediaProvider> providers) {
        this.providers = providers;
    }

    public SearchResult search(String query, int page, int size, MediaType type, Integer year,
                               String genre, BigDecimal minimumRating) {
        List<MediaProvider> selectedProviders = providers.stream()
                .filter(provider -> type == null || provider.supports(type)).toList();
        List<CompletableFuture<ProviderResults>> requests = selectedProviders.stream()
                .map(provider -> CompletableFuture.supplyAsync(
                        () -> fetchPages(provider, query, page, size, type, year))
                        .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS))
                .toList();

        List<ProviderResults> providerResults = new ArrayList<>();
        int failures = 0;
        for (CompletableFuture<ProviderResults> request : requests) {
            try {
                providerResults.add(request.join());
            } catch (CompletionException exception) {
                failures++;
            }
        }
        if (providerResults.isEmpty() && failures > 0) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_PROVIDER_UNAVAILABLE",
                    "Media search providers are temporarily unavailable");
        }

        List<MediaRecord> interleaved = interleave(providerResults.stream()
                .map(ProviderResults::results).toList());
        List<MediaRecord> pageRecords = interleaved.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
        List<MediaRecord> filtered = pageRecords.stream()
                .filter(record -> year == null || year.equals(record.releaseYear()))
                .filter(record -> genre == null || genre.isBlank() ||
                        (record.genre() != null && record.genre().toLowerCase(Locale.ROOT)
                                .contains(genre.toLowerCase(Locale.ROOT))))
                .filter(record -> minimumRating == null ||
                        (record.externalRating() != null && record.externalRating().compareTo(minimumRating) >= 0))
                .toList();
        long total = providerResults.stream().mapToLong(ProviderResults::totalElements).sum();
        int totalPages = (int) Math.ceil((double) total / size);
        return new SearchResult(filtered, page, size, total, totalPages);
    }

    public List<MediaRecord> suggestions(String query) {
        return search(query, 0, 8, null, null, null, null).content();
    }

    public MediaRecord findByExternalId(String providerId, String externalId, MediaType requestedType) {
        MediaProvider provider = providers.stream()
                .filter(candidate -> candidate.providerId().equalsIgnoreCase(providerId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found"));
        try {
            if (requestedType != null) {
                MediaRecord record = provider.findByExternalId(externalId, requestedType);
                if (record == null) throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found");
                return record;
            }
            for (MediaType type : MediaType.values()) {
                if (!provider.supports(type)) continue;
                MediaRecord record = provider.findByExternalId(externalId, type);
                if (record != null) return record;
            }
        } catch (RestClientException exception) {
            throw providerUnavailable();
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found");
    }

    private static ApiException providerUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_PROVIDER_UNAVAILABLE",
                "The media provider is temporarily unavailable");
    }

    private static ProviderResults fetchPages(MediaProvider provider, String query, int requestedPage, int size,
                                              MediaType type, Integer year) {
        ProviderPage firstPage = provider.search(query, 0, size, type, year);
        List<MediaRecord> records = new ArrayList<>(firstPage.results());
        int pageCapacity = firstPage.results().isEmpty() ? size : firstPage.results().size();
        int pageCount = firstPage.totalPages() > 0 ? firstPage.totalPages() :
                (int) Math.ceil((double) firstPage.totalElements() / pageCapacity);
        for (int page = 1; page <= requestedPage && page < pageCount; page++) {
            try {
                records.addAll(provider.search(query, page, size, type, year).results());
            } catch (RestClientException exception) {
                break;
            }
        }
        return new ProviderResults(records, firstPage.totalElements());
    }

    private static List<MediaRecord> interleave(List<List<MediaRecord>> pages) {
        List<MediaRecord> result = new ArrayList<>();
        int index = 0;
        boolean hasMore = true;
        while (hasMore) {
            hasMore = false;
            for (List<MediaRecord> page : pages) {
                if (index < page.size()) {
                    result.add(page.get(index));
                    hasMore = true;
                }
            }
            index++;
        }
        return result;
    }

    private record ProviderResults(List<MediaRecord> results, long totalElements) { }

    public record SearchResult(List<MediaRecord> content, int page, int size,
                               long totalElements, int totalPages) { }
}
