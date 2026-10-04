package com.guilhermedev.librarymedia.media;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenLibraryMediaProviderTests {
    private WireMockServer wireMock;
    private OpenLibraryMediaProvider provider;

    @BeforeEach
    void startProviderStub() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        provider = new OpenLibraryMediaProvider(RestClient.builder(),
                "http://localhost:" + wireMock.port(), "LibraryMediaTests/1.0");
    }

    @AfterEach
    void stopProviderStub() {
        wireMock.stop();
    }

    @Test
    void appliesYearFilterUsingSearchQuerySyntax() {
        wireMock.stubFor(get(urlPathEqualTo("/search.json"))
                .withQueryParam("q", equalTo("(dune) AND first_publish_year:1965"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "numFound": 2,
                                  "docs": [
                                    {"key": "/works/OL1W", "title": "Dune", "first_publish_year": 1965},
                                    {"key": "/works/OL2W", "title": "Dune", "first_publish_year": 2010}
                                  ]
                                }
                                """)));

        ProviderPage page = provider.search("dune", 0, 10, MediaType.BOOK, 1965);

        assertEquals("openlibrary", provider.providerId());
        assertEquals(2, page.results().size());
        assertEquals("OL1W", page.results().get(0).externalId());
        assertEquals(1965, page.results().get(0).releaseYear());
        assertNull(page.results().get(0).genre());
        assertEquals(2, page.totalElements());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/search.json"))
                .withQueryParam("first_publish_year", absent()));
    }
}
