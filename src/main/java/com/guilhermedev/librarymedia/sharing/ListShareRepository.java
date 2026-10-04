package com.guilhermedev.librarymedia.sharing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListShareRepository extends JpaRepository<ListShare, Long> {
    Optional<ListShare> findByListId(Long listId);
    Optional<ListShare> findByTokenAndActiveTrue(String token);
}
