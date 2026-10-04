package com.guilhermedev.librarymedia.library;

import com.guilhermedev.librarymedia.media.MediaEntity;
import com.guilhermedev.librarymedia.user.UserAccount;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_media", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_media", columnNames = {"user_id", "media_id"}))
public class UserMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaEntity media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsumptionStatus status;

    @Column(columnDefinition = "TINYINT")
    private Integer rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserMedia() {
    }

    public UserMedia(UserAccount user, MediaEntity media, ConsumptionStatus status, Integer rating) {
        this.user = user;
        this.media = media;
        this.status = status;
        if (rating != null) {
            rate(rating);
        }
    }

    public void changeStatus(ConsumptionStatus status) {
        this.status = java.util.Objects.requireNonNull(status);
    }

    public void rate(Integer rating) {
        if (status != ConsumptionStatus.DONE) {
            throw new IllegalStateException("Only completed media can be rated");
        }
        if (rating == null || rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 1 and 10");
        }
        this.rating = rating;
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public MediaEntity getMedia() { return media; }
    public ConsumptionStatus getStatus() { return status; }
    public Integer getRating() { return rating; }
}
