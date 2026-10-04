package com.guilhermedev.librarymedia.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.guilhermedev.librarymedia.library.ConsumptionStatus;
import com.guilhermedev.librarymedia.media.MediaType;

import java.util.List;

public interface ListItemRepository extends JpaRepository<ListItem, ListItemId> {
    boolean existsByListIdAndUserMediaId(Long listId, Long userMediaId);
    long countByUserMediaIdAndListIdNot(Long userMediaId, Long listId);
    @Query("select item from ListItem item where item.list.id = :listId " +
            "and item.list.user.id = :userId and item.userMedia.user.id = :userId")
    List<ListItem> findAllByListIdAndUserId(@Param("listId") Long listId, @Param("userId") Long userId);
    @Query("select count(item) from ListItem item where item.list.id = :listId " +
            "and item.list.user.id = :userId and item.userMedia.user.id = :userId")
    long countByListIdAndUserId(@Param("listId") Long listId, @Param("userId") Long userId);
    @Query("select item from ListItem item where item.list.id = :listId " +
            "and item.list.user.id = :userId and item.userMedia.user.id = :userId")
    Page<ListItem> findOwnedByListId(@Param("listId") Long listId, @Param("userId") Long userId,
                                     Pageable pageable);
    @Query("select item from ListItem item where item.list.id = :listId " +
            "and item.list.user.id = :userId and item.userMedia.user.id = :userId " +
            "and (:type is null or item.userMedia.media.type = :type) " +
            "and (:status is null or item.userMedia.status = :status)")
    Page<ListItem> findFilteredByListId(@Param("listId") Long listId, @Param("userId") Long userId,
                                        @Param("type") MediaType type,
                                        @Param("status") ConsumptionStatus status, Pageable pageable);
    List<ListItem> findAllByUserMediaId(Long userMediaId);
    void deleteByListIdAndUserMediaId(Long listId, Long userMediaId);
    void deleteAllByUserMediaId(Long userMediaId);

    @Query("select item.userMedia.id from ListItem item where item.list.id = :listId " +
            "and item.userMedia.user.id = :userId " +
            "and (select count(other.id) from ListItem other where other.userMedia.id = item.userMedia.id and other.list.id <> :listId) = 0")
    List<Long> findExclusiveEntryIds(@Param("listId") Long listId, @Param("userId") Long userId);
}
