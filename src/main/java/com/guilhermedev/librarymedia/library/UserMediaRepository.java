package com.guilhermedev.librarymedia.library;

import com.guilhermedev.librarymedia.media.MediaType;
import com.guilhermedev.librarymedia.list.ListItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMediaRepository extends JpaRepository<UserMedia, Long>, JpaSpecificationExecutor<UserMedia> {
    Optional<UserMedia> findByIdAndUserId(Long id, Long userId);

    @Query("select entry from UserMedia entry join fetch entry.media media " +
            "where entry.user.id = :userId and media.provider = :provider and media.externalId = :externalId")
    Optional<UserMedia> findByUserIdAndProviderAndExternalId(@Param("userId") Long userId,
                                                              @Param("provider") String provider,
                                                              @Param("externalId") String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from UserMedia entry where entry.id = :entryId and entry.user.id = :userId")
    Optional<UserMedia> findLockedByIdAndUserId(@Param("entryId") Long entryId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from UserMedia entry where entry.user.id = :userId and entry.media.id = :mediaId")
    Optional<UserMedia> findLockedByUserIdAndMediaId(@Param("userId") Long userId,
                                                      @Param("mediaId") Long mediaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from UserMedia entry join ListItem item on item.userMedia = entry " +
            "where item.list.id = :listId and entry.user.id = :userId order by entry.id")
    List<UserMedia> lockAllByListIdAndUserId(@Param("listId") Long listId, @Param("userId") Long userId);

    Page<UserMedia> findAllByUserIdAndStatusAndRatingIsNotNullAndMediaType(
            Long userId, ConsumptionStatus status, MediaType type, Pageable pageable);
}
