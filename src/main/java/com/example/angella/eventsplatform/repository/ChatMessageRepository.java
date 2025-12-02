package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findAllByEventId(Long eventId, Pageable pageable);
    boolean existsByIdAndEventIdAndAuthorId(Long id, Long eventId, Long authorId);

    boolean existsByIdAndAuthorId(Long messageId, Long userId);

    @EntityGraph(attributePaths = {"images", "author"})
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.id = :id")
    Optional<ChatMessage> findByIdWithImages(@Param("id") Long id);
}