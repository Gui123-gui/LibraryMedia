package com.guilhermedev.librarymedia.media;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "media", uniqueConstraints = @UniqueConstraint(
        name = "uq_media_provider_external", columnNames = {"provider", "external_id"}))
public class MediaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType type;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "release_year", columnDefinition = "SMALLINT")
    private Short releaseYear;

    @Column(length = 255)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "external_rating", precision = 3, scale = 1)
    private BigDecimal externalRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MediaEntity() {
    }

    public MediaEntity(MediaType type, String provider, String externalId, String title,
                       Integer releaseYear, String genre, String synopsis, String coverUrl,
                       BigDecimal externalRating) {
        this.type = type;
        this.provider = provider;
        this.externalId = externalId;
        this.title = title;
        this.releaseYear = releaseYear == null ? null : releaseYear.shortValue();
        this.genre = genre;
        this.synopsis = synopsis;
        this.coverUrl = coverUrl;
        this.externalRating = externalRating;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public MediaType getType() { return type; }
    public String getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public Integer getReleaseYear() { return releaseYear == null ? null : releaseYear.intValue(); }
    public String getGenre() { return genre; }
    public String getSynopsis() { return synopsis; }
    public String getCoverUrl() { return coverUrl; }
    public BigDecimal getExternalRating() { return externalRating; }
}
