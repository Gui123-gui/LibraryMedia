package com.guilhermedev.librarymedia.media;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenLibraryMediaProvider implements MediaProvider {
    private final RestClient restClient;

    public OpenLibraryMediaProvider(RestClient.Builder builder,
                                   @Value("${app.media.open-library.base-url:https://openlibrary.org}") String baseUrl,
                                   @Value("${app.media.open-library.user-agent:PersonalMediaLibrary/1.0}")
                                   String userAgent) {
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    @Override
    public String providerId() { return "openlibrary"; }

    @Override
    public boolean supports(MediaType type) { return type == MediaType.BOOK; }

    @Override
    @Cacheable(cacheNames = "providerSearch",
            key = "'openlibrary:' + #query + ':' + #page + ':' + #size + ':' + #year")
    public ProviderPage search(String query, int page, int size, MediaType type, Integer year) {
        String searchQuery = year == null ? query : "(" + query + ") AND first_publish_year:" + year;
        JsonNode root = restClient.get().uri(uri -> uri.path("/search.json")
                        .queryParam("q", searchQuery).queryParam("page", page + 1).queryParam("limit", size)
                        .queryParam("fields", "key,title,author_name,first_publish_year,subject,cover_i,ratings_average")
                        .build())
                .retrieve().body(JsonNode.class);
        if (root == null) return new ProviderPage(List.of(), 0);
        List<MediaRecord> records = new ArrayList<>();
        for (JsonNode item : root.path("docs")) records.add(mapItem(item, false));
        long total = root.path("numFound").asLong(records.size());
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new ProviderPage(records, total, totalPages);
    }

    @Override
    @Cacheable(cacheNames = "providerDetails", key = "'openlibrary:' + #externalId")
    public MediaRecord findByExternalId(String externalId, MediaType type) {
        if (type != MediaType.BOOK) return null;
        String workId = externalId.startsWith("/works/") ? externalId.substring(7) : externalId;
        JsonNode item;
        try {
            item = restClient.get().uri("/works/{id}.json", workId).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound ignored) {
            return null;
        }
        if (item == null || item.isEmpty()) return null;
        return mapItem(item, true);
    }

    private MediaRecord mapItem(JsonNode item, boolean detail) {
        String key = text(item, "key");
        String externalId = key == null ? null : key.substring(key.lastIndexOf('/') + 1);
        String title = text(item, "title");
        Integer year = item.path("first_publish_year").canConvertToInt()
                ? validYear(item.path("first_publish_year").asInt()) : null;
        JsonNode subjects = item.path("subject");
        if (detail && !subjects.isArray()) subjects = item.path("subjects");
        List<String> genres = new ArrayList<>();
        if (subjects.isArray()) {
            for (JsonNode subject : subjects) {
                if (genres.size() == 3) break;
                String name = subject.isTextual() ? subject.asText() : subject.path("name").asText(null);
                if (name != null && !name.isBlank()) genres.add(name);
            }
        }
        JsonNode description = item.path("description");
        String synopsis = description.isTextual() ? description.asText() :
                description.isObject() ? description.path("value").asText(null) : null;
        String coverUrl = null;
        if (item.path("cover_i").canConvertToInt()) {
            coverUrl = "https://covers.openlibrary.org/b/id/" + item.path("cover_i").asInt() + "-L.jpg";
        } else if (item.path("covers").isArray() && !item.path("covers").isEmpty()) {
            coverUrl = "https://covers.openlibrary.org/b/id/" + item.path("covers").get(0).asInt() + "-L.jpg";
        }
        JsonNode ratingValue = item.path("ratings_average");
        BigDecimal rating = ratingValue.isNumber()
                ? BigDecimal.valueOf(ratingValue.asDouble()).setScale(1, java.math.RoundingMode.HALF_UP) : null;
        return new MediaRecord(providerId(), externalId, MediaType.BOOK.name(), title, year,
                genres.isEmpty() ? null : String.join(", ", genres), synopsis, coverUrl, rating);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static Integer validYear(int year) {
        return year > 0 && year <= Year.now().getValue() + 5 ? year : null;
    }
}
