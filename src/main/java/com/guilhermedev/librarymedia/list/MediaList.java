package com.guilhermedev.librarymedia.list;

import com.guilhermedev.librarymedia.user.UserAccount;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "lists")
public class MediaList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_favorites", nullable = false)
    private boolean favorites;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MediaList() {
    }

    public MediaList(UserAccount user, String name, boolean favorites) {
        this.user = user;
        this.name = name;
        this.favorites = favorites;
    }

    public void rename(String name) {
        if (!favorites) this.name = name;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getName() { return name; }
    public boolean isFavorites() { return favorites; }
}
