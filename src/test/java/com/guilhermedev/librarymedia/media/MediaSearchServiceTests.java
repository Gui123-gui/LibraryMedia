package com.guilhermedev.librarymedia.media;

import com.guilhermedev.librarymedia.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaSearchServiceTests {
    @Test
    void searchContinuesWithAvailableProviderAndSuggestionsStayWithinEight() {
        MediaProvider available = mock(MediaProvider.class);
        MediaProvider unavailable = mock(MediaProvider.class);
        when(available.supports(nullable(MediaType.class))).thenReturn(true);
        when(unavailable.supports(nullable(MediaType.class))).thenReturn(true);
        List<MediaRecord> records = java.util.stream.IntStream.range(0, 12)
                .mapToObj(index -> new MediaRecord("tmdb", "id-" + index, "MOVIE",
                        "Movie " + index, 2024, null, null, null, null))
                .toList();
        when(available.search(anyString(), anyInt(), anyInt(), nullable(MediaType.class), nullable(Integer.class)))
                .thenReturn(new ProviderPage(records, records.size()));
        when(unavailable.search(anyString(), anyInt(), anyInt(), nullable(MediaType.class), nullable(Integer.class)))
                .thenThrow(new ResourceAccessException("offline"));

        MediaSearchService service = new MediaSearchService(List.of(available, unavailable));

        assertEquals(8, service.suggestions("movie").size());
        assertEquals(12, service.search("movie", 0, 20, null, null, null, null).content().size());
    }

    @Test
    void searchReturnsProviderUnavailableOnlyWhenEveryProviderFails() {
        MediaProvider unavailable = mock(MediaProvider.class);
        when(unavailable.supports(nullable(MediaType.class))).thenReturn(true);
        when(unavailable.search(anyString(), anyInt(), anyInt(), nullable(MediaType.class), nullable(Integer.class)))
                .thenThrow(new ResourceAccessException("offline"));

        ApiException error = assertThrows(ApiException.class,
                () -> new MediaSearchService(List.of(unavailable))
                        .search("movie", 0, 20, null, null, null, null));

        assertEquals("EXTERNAL_PROVIDER_UNAVAILABLE", error.getCode());
        assertTrue(error.getMessage().contains("temporarily"));
    }

    @Test
    void mergedPagesDoNotSkipResultsFromTheFirstProviderPages() {
        MediaProvider first = pagedProvider("first");
        MediaProvider second = pagedProvider("second");
        MediaSearchService service = new MediaSearchService(List.of(first, second));

        MediaSearchService.SearchResult page = service.search("movie", 1, 20, null, null, null, null);

        assertEquals("first-10", page.content().get(0).title());
        assertEquals("second-10", page.content().get(1).title());
        assertEquals(120, page.totalElements());
    }

    private MediaProvider pagedProvider(String prefix) {
        MediaProvider provider = mock(MediaProvider.class);
        when(provider.supports(nullable(MediaType.class))).thenReturn(true);
        when(provider.search(anyString(), anyInt(), anyInt(), nullable(MediaType.class), nullable(Integer.class)))
                .thenAnswer(invocation -> {
                    int page = invocation.getArgument(1);
                    List<MediaRecord> records = java.util.stream.IntStream.range(0, 20)
                            .mapToObj(index -> {
                                int number = page * 20 + index;
                                return new MediaRecord(prefix, prefix + "-" + number, "MOVIE",
                                        prefix + "-" + number, 2024, null, null, null, null);
                            })
                            .toList();
                    return new ProviderPage(records, 60);
                });
        return provider;
    }
}
