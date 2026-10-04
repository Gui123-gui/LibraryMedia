package com.guilhermedev.librarymedia.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
    Optional<MediaEntity> findByProviderAndExternalId(String provider, String externalId);
}
