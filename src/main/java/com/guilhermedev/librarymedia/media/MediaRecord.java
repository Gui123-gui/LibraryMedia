package com.guilhermedev.librarymedia.media;

import java.math.BigDecimal;

public record MediaRecord(String provider, String externalId, String type, String title,
                          Integer releaseYear, String genre, String synopsis, String coverUrl,
                          BigDecimal externalRating) {
}
