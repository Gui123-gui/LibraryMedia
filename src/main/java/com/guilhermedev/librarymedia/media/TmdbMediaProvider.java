package com.guilhermedev.librarymedia.media;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TmdbMediaProvider implements MediaProvider {
    private static final Map<Integer, String> GENRES = Map.ofEntries(
            Map.entry(28, "Action"), Map.entry(12, "Adventure"), Map.entry(16, "Animation"),
            Map.entry(35, "Comedy"), Map.entry(80, "Crime"), Map.entry(18, "Drama"),
            Map.entry(14, "Fantasy"), Map.entry(27, "Horror"), Map.entry(878, "Science Fiction"),
            Map.entry(10749, "Romance"), Map.entry(53, "Thriller"), Map.entry(10751, "Family"),
            Map.entry(99, "Documentary"), Map.entry(9648, "Mystery"));

    private final RestClient restClient;
    private final String apiKey;

    public TmdbMediaProvider(RestClient.Builder builder,
                             @Value("${app.media.tmdb.api-key}") String apiKey,
                             @Value("${app.media.tmdb.base-url:https://api.themoviedb.org/3}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public String providerId() { return "tmdb"; }

    @Override
    public boolean supports(MediaType type) { return type == MediaType.MOVIE || type == MediaType.SERIES; }

    @Override
    @Cacheable(cacheNames = "providerSearch",
            key = "'tmdb:' + #query + ':' + #page + ':' + #size + ':' + #type + ':' + #year")
    public ProviderPage search(String query, int page, int size, MediaType type, Integer year) {
        List<MediaRecord> movies = List.of();
        List<MediaRecord> series = List.of();
        long total = 0;
        int totalPages = 0;
        if (type == null || type == MediaType.MOVIE) {
            ProviderPage moviePage = searchType(query, page + 1, size, "movie", MediaType.MOVIE, year);
            movies = moviePage.results();
            total += moviePage.totalElements();
            totalPages = pagesFor(moviePage.totalElements(), size);
        }
        if (type == null || type == MediaType.SERIES) {
            ProviderPage seriesPage = searchType(query, page + 1, size, "tv", MediaType.SERIES, year);
            series = seriesPage.results();
            total += seriesPage.totalElements();
            totalPages = Math.max(totalPages, pagesFor(seriesPage.totalElements(), size));
        }
        List<MediaRecord> results = type == null ? interleave(movies, series) :
                type == MediaType.MOVIE ? movies : series;
        if (type != null) results = results.stream().limit(size).toList();
        return new ProviderPage(results, total, totalPages);
    }

    private static int pagesFor(long total, int size) {
        return total == 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    private static List<MediaRecord> interleave(List<MediaRecord> first, List<MediaRecord> second) {
        List<MediaRecord> results = new ArrayList<>(first.size() + second.size());
        for (int index = 0; index < Math.max(first.size(), second.size()); index++) {
            if (index < first.size()) results.add(first.get(index));
            if (index < second.size()) results.add(second.get(index));
        }
        return results;
    }

    private ProviderPage searchType(String query, int providerPage, int size, String path, MediaType type, Integer year) {
        JsonNode root = restClient.get().uri(uri -> uri.path("/search/" + path)
                        .queryParam("api_key", apiKey).queryParam("query", query)
                        .queryParam("page", providerPage).queryParam("include_adult", false)
                        .queryParamIfPresent(type == MediaType.MOVIE ? "primary_release_year" : "first_air_date_year",
                                java.util.Optional.ofNullable(year)).build())
                .retrieve().body(JsonNode.class);
        return parsePage(root, type);
    }

    @Override
    @Cacheable(cacheNames = "providerDetails", key = "'tmdb:' + #externalId + ':' + #type")
    public MediaRecord findByExternalId(String externalId, MediaType type) {
        if (type != MediaType.MOVIE && type != MediaType.SERIES) return null;
        String path = type == MediaType.MOVIE ? "movie" : "tv";
        JsonNode detail;
        try {
            detail = restClient.get().uri(uri -> uri.path("/" + path + "/" + externalId)
                            .queryParam("api_key", apiKey).build())
                    .retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound ignored) {
            return null;
        }
        if (detail == null || detail.isEmpty()) return null;
        return mapItem(detail, type);
    }

    private ProviderPage parsePage(JsonNode root, MediaType type) {
        if (root == null) return new ProviderPage(List.of(), 0);
        List<MediaRecord> records = new ArrayList<>();
        for (JsonNode item : root.path("results")) records.add(mapItem(item, type));
        return new ProviderPage(records, root.path("total_results").asLong(records.size()));
    }

    private MediaRecord mapItem(JsonNode item, MediaType type) {
        String title = text(item, type == MediaType.MOVIE ? "title" : "name");
        String date = text(item, type == MediaType.MOVIE ? "release_date" : "first_air_date");
        Integer year = parseYear(date);
        List<String> genreNames = new ArrayList<>();
        JsonNode genres = item.path("genres");
        if (genres.isArray() && !genres.isEmpty()) {
            for (JsonNode genre : genres) genreNames.add(text(genre, "name"));
        } else {
            for (JsonNode genreId : item.path("genre_ids")) {
                String name = GENRES.get(genreId.asInt());
                if (name != null) genreNames.add(name);
            }
        }
        String posterPath = text(item, "poster_path");
        String cover = posterPath == null ? null : "https://image.tmdb.org/t/p/w500" + posterPath;
        BigDecimal rating = item.path("vote_average").isNumber()
                ? BigDecimal.valueOf(item.path("vote_average").asDouble()).setScale(1, java.math.RoundingMode.HALF_UP)
                : null;
        return new MediaRecord(providerId(), item.path("id").asText(), type.name(), title, year,
                genreNames.isEmpty() ? null : String.join(", ", genreNames),
                text(item, "overview"), cover, rating);
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static Integer parseYear(String value) {
        if (value == null || value.length() < 4) return null;
        try {
            int year = Integer.parseInt(value.substring(0, 4));
            return year >= 1 && year <= Year.now().getValue() + 5 ? year : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
