package com.guilhermedev.librarymedia.sharing;

import com.guilhermedev.librarymedia.list.MediaList;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "shares")
public class ListShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false, unique = true)
    private MediaList list;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ListShare() {
    }

    public ListShare(MediaList list, String token) {
        this.list = list;
        this.token = token;
        this.active = true;
    }

    public void setActive(boolean active) { this.active = active; }
    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public MediaList getList() { return list; }
    public String getToken() { return token; }
    public boolean isActive() { return active; }
}
