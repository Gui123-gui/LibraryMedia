package com.guilhermedev.librarymedia.media;

public interface MediaProvider {
    String providerId();
    boolean supports(MediaType type);
    ProviderPage search(String query, int page, int size, MediaType type, Integer year);
    MediaRecord findByExternalId(String externalId, MediaType type);
}
