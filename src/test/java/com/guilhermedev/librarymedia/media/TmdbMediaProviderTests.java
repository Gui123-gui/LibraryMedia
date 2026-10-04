package com.guilhermedev.librarymedia.media;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TmdbMediaProviderTests {
    private WireMockServer wireMock;
    private TmdbMediaProvider provider;

    @BeforeEach
    void startProviderStub() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        provider = new TmdbMediaProvider(RestClient.builder(), "test-key",
                "http://localhost:" + wireMock.port());
    }

    @AfterEach
    void stopProviderStub() {
        wireMock.stop();
    }

    @Test
    void combinedMovieAndSeriesSearchReturnsBothTypesInOnePage() {
        wireMock.stubFor(get(urlPathEqualTo("/search/movie"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"total_results": 1, "results": [
                                  {"id": 10, "title": "A Movie", "release_date": "2024-01-01"}
                                ]}
                                """)));
        wireMock.stubFor(get(urlPathEqualTo("/search/tv"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"total_results": 1, "results": [
                                  {"id": 20, "name": "A Series", "first_air_date": "2023-01-01"}
                                ]}
                                """)));

        ProviderPage result = provider.search("a", 0, 2, null, null);

        assertEquals("tmdb", provider.providerId());
        assertEquals(2, result.results().size());
        assertEquals(MediaType.MOVIE.name(), result.results().get(0).type());
        assertEquals(MediaType.SERIES.name(), result.results().get(1).type());
        assertEquals(2, result.totalElements());
    }
}
