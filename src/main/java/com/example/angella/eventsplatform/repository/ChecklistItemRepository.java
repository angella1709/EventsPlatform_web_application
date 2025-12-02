package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findAllByEventId(Long eventId);

    boolean existsByIdAndEventId(Long id, Long eventId);

    boolean existsByIdAndCreatedById(Long itemId, Long userId);

    @Query("SELECT COUNT(c) FROM ChecklistItem c WHERE c.event.id = :eventId AND c.completed = true")
    long countCompletedItems(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(c) FROM ChecklistItem c WHERE c.event.id = :eventId")
    long countTotalItems(@Param("eventId") Long eventId);

    long countByCompletedTrue();
}