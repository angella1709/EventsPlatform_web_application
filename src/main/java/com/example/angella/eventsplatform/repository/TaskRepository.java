package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    boolean existsByIdAndEventIdAndCreatorId(Long id, Long eventId, Long creatorId);

    boolean existsByIdAndCreatorId(Long taskId, Long userId);

    List<Task> findAllByEventId(Long eventId);

    long countByCompletedTrue();
}
