package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.Task;
import com.example.angella.eventsplatform.entity.Event;
import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.TaskRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventAccessService eventAccessService;

    public List<Task> getTasksForEvent(Long eventId) {
        return taskRepository.findAllByEventId(eventId);
    }

    public Task createTask(String description, Long eventId, Long userId, Long assignedUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if (!eventAccessService.hasParticipant(eventId, userId)) {
            throw new AccessDeniedException("Only event participants can create tasks");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        User assignedUser = null;
        if (assignedUserId != null) {
            assignedUser = userRepository.findById(assignedUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Assigned user not found"));
            // Проверяем, что назначенный пользователь является участником события
            if (!eventAccessService.hasParticipant(eventId, assignedUserId)) {
                throw new AccessDeniedException("Assigned user must be event participant");
            }
        }

        Task task = new Task();
        task.setDescription(description);
        task.setEvent(event);
        task.setCreator(user);
        task.setAssignedUser(assignedUser);
        task.setCompleted(false);

        return taskRepository.save(task);
    }

    public Task updateTask(Long taskId, String newDescription, Boolean completed,
                           Long assignedUserId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (!task.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("Only task creator can update the task");
        }

        if (newDescription != null) {
            task.setDescription(newDescription);
        }
        if (completed != null) {
            task.setCompleted(completed);
        }
        if (assignedUserId != null) {
            User assignedUser = userRepository.findById(assignedUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Assigned user not found"));
            if (!eventAccessService.hasParticipant(task.getEvent().getId(), assignedUserId)) {
                throw new AccessDeniedException("Assigned user must be event participant");
            }
            task.setAssignedUser(assignedUser);
        }

        return taskRepository.save(task);
    }

    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Task with id {0} not found!", taskId)
                ));

        if (!task.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("Only task creator can delete the task");
        }

        taskRepository.deleteById(taskId);
    }

    public boolean isTaskCreator(Long taskId, Long userId) {
        return taskRepository.existsByIdAndCreatorId(taskId, userId);
    }

    public Long getTotalTasksCount() {
        return taskRepository.count();
    }

    public Integer getCompletedTasksPercentage() {
        Long total = taskRepository.count();
        Long completed = taskRepository.countByCompletedTrue();

        if (total == 0) return 0;
        return (int) ((completed * 100) / total);
    }

    public Task toggleTaskCompletion(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        // Проверяем, что пользователь является участником события
        if (!eventAccessService.hasParticipant(task.getEvent().getId(), userId)) {
            throw new AccessDeniedException("Only event participants can toggle task completion");
        }

        task.setCompleted(!task.isCompleted());
        return taskRepository.save(task);
    }
}