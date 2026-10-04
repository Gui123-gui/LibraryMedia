package com.guilhermedev.librarymedia.list;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaListRepository extends JpaRepository<MediaList, Long> {
    List<MediaList> findAllByUserIdOrderByFavoritesDescNameAsc(Long userId);
    Optional<MediaList> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select list from MediaList list where list.id = :listId and list.user.id = :userId")
    Optional<MediaList> findLockedByIdAndUserId(@Param("listId") Long listId, @Param("userId") Long userId);

    Optional<MediaList> findByUserIdAndFavoritesTrue(Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
