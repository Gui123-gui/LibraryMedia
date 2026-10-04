package com.guilhermedev.librarymedia.library;

import com.guilhermedev.librarymedia.media.MediaType;

public enum ConsumptionStatus {
    WANT,
    IN_PROGRESS,
    DONE;

    public String labelFor(MediaType type) {
        if (type == MediaType.BOOK) {
            return switch (this) {
                case WANT -> "QUERO_LER";
                case IN_PROGRESS -> "LENDO";
                case DONE -> "LIDO";
            };
        }
        return switch (this) {
            case WANT -> "QUERO_ASSISTIR";
            case IN_PROGRESS -> "ASSISTINDO";
            case DONE -> "ASSISTIDO";
        };
    }
}
