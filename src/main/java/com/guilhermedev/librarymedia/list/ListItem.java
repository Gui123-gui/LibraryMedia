package com.guilhermedev.librarymedia.list;

import com.guilhermedev.librarymedia.library.UserMedia;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "list_items")
@IdClass(ListItemId.class)
public class ListItem {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private MediaList list;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_media_id", nullable = false)
    private UserMedia userMedia;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    protected ListItem() {
    }

    public ListItem(MediaList list, UserMedia userMedia) {
        this.list = list;
        this.userMedia = userMedia;
    }

    public MediaList getList() { return list; }
    public UserMedia getUserMedia() { return userMedia; }
}
