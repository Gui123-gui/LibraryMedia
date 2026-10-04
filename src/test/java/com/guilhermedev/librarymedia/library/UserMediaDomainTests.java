package com.guilhermedev.librarymedia.library;

import com.guilhermedev.librarymedia.media.MediaEntity;
import com.guilhermedev.librarymedia.media.MediaType;
import com.guilhermedev.librarymedia.user.UserAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserMediaDomainTests {

    @Test
    void onlyCompletedMediaCanBeRatedAndRatingSurvivesStatusChanges() {
        UserAccount user = new UserAccount("Reader", "reader@example.com", "hashed-password");
        MediaEntity media = new MediaEntity(MediaType.BOOK, "openlibrary", "OL1W", "Dune",
                1965, null, null, null, BigDecimal.ZERO);
        UserMedia entry = new UserMedia(user, media, ConsumptionStatus.WANT, null);

        assertThrows(IllegalStateException.class, () -> entry.rate(8));

        entry.changeStatus(ConsumptionStatus.DONE);
        entry.rate(8);
        entry.changeStatus(ConsumptionStatus.IN_PROGRESS);
        assertEquals(8, entry.getRating());

        entry.changeStatus(ConsumptionStatus.DONE);
        assertThrows(IllegalArgumentException.class, () -> entry.rate(11));
    }

    @Test
    void statusLabelsDependOnMediaType() {
        assertEquals("QUERO_LER", ConsumptionStatus.WANT.labelFor(MediaType.BOOK));
        assertEquals("ASSISTIDO", ConsumptionStatus.DONE.labelFor(MediaType.MOVIE));
        assertEquals("ASSISTINDO", ConsumptionStatus.IN_PROGRESS.labelFor(MediaType.SERIES));
    }
}
