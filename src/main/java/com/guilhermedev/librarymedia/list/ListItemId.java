package com.guilhermedev.librarymedia.list;

import java.io.Serializable;
import java.util.Objects;

public class ListItemId implements Serializable {
    private Long list;
    private Long userMedia;

    public ListItemId() {
    }

    public ListItemId(Long list, Long userMedia) {
        this.list = list;
        this.userMedia = userMedia;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ListItemId that)) return false;
        return Objects.equals(list, that.list) && Objects.equals(userMedia, that.userMedia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, userMedia);
    }
}
