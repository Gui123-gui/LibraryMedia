package com.guilhermedev.librarymedia.media;

import java.util.List;

public record ProviderPage(List<MediaRecord> results, long totalElements, int totalPages) {
    public ProviderPage(List<MediaRecord> results, long totalElements) {
        this(results, totalElements, 0);
    }
}
