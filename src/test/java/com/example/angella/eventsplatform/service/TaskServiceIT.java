package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class TaskServiceIT extends ServiceIntegrationTest {

    // Сервисы и репозитории для работы с задачами, событиями и пользователями
    @Autowired private TaskService taskService;
    @Autowired private EventService eventService;
    @Autowired private UserService userService;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryService categoryService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Подготовка тестового пользователя
        testUser = new User();
        testUser.setUsername("taskuser");
        testUser.setEmail("task@example.com");
        testUser.setPassword("password");
        testUser = userService.registerUser(testUser);

        // Подготовка тестового события
        testEvent = buildTestEvent();
        testEvent = eventService.create(testEvent, testUser.getId());
    }

    private Event buildTestEvent() {
        // Создание объекта события с обязательными полями
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("Test event description for task management");
        event.setStartTime(Instant.now());
        event.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));

        // Настройка локации
        Location location = new Location();
        location.setCity("Test City");
        location.setStreet("Test Street");
        location = locationRepository.save(location);
        event.setLocation(location);

        event.setCreator(testUser);
        return event;
    }

    @Test
    void createTask_ShouldSaveTask() {
        // Тест создания задачи
        Task task = taskService.createTask("Test task", testEvent.getId(), testUser.getId(), null);

        // Проверки корректности сохранения
        assertNotNull(task.getId());
        assertEquals("Test task", task.getDescription());
        assertFalse(task.isCompleted());
        assertEquals(testUser.getId(), task.getCreator().getId());
    }

    @Test
    void createTask_WithAssignedUser_ShouldSaveTask() {
        // Тест создания задачи с назначенным пользователем
        User assignedUser = createTestUser("assigneduser");
        eventService.addParticipant(testEvent.getId(), assignedUser.getId());

        Task task = taskService.createTask("Task with assignment", testEvent.getId(), testUser.getId(), assignedUser.getId());

        assertNotNull(task.getId());
        assertEquals("Task with assignment", task.getDescription());
        assertEquals(assignedUser.getId(), task.getAssignedUser().getId());
    }

    @Test
    void createTask_ShouldThrowWhenNotParticipant() {
        // Тест проверки прав доступа при создании задачи
        User nonParticipant = new User();
        nonParticipant.setUsername("nonparticipant");
        nonParticipant.setEmail("non@example.com");
        nonParticipant.setPassword("password");
        User registeredNonParticipant = userService.registerUser(nonParticipant);

        assertThrows(AccessDeniedException.class, () -> {
            taskService.createTask("Test", testEvent.getId(), registeredNonParticipant.getId(), null);
        });
    }

    @Test
    void createTask_ShouldThrowWhenAssignedUserNotParticipant() {
        // Тест проверки что назначенный пользователь должен быть участником
        User nonParticipant = createTestUser("nonparticipant2");

        assertThrows(AccessDeniedException.class, () -> {
            taskService.createTask("Test", testEvent.getId(), testUser.getId(), nonParticipant.getId());
        });
    }

    @Test
    void getTasksForEvent_ShouldReturnAllTasks() {
        // Тест получения списка задач
        taskService.createTask("Task 1", testEvent.getId(), testUser.getId(), null);
        taskService.createTask("Task 2", testEvent.getId(), testUser.getId(), null);

        var tasks = taskService.getTasksForEvent(testEvent.getId());
        assertEquals(3, tasks.size());
    }

    @Test
    void getTasksForEvent_ShouldReturnEmptyListForNonExistentEvent() {
        // Тест получения задач для несуществующего события
        var tasks = taskService.getTasksForEvent(999L);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void updateTask_ShouldUpdateFields() {
        // Тест обновления задачи
        Task task = taskService.createTask("Original task", testEvent.getId(), testUser.getId(), null);

        Task updated = taskService.updateTask(
                task.getId(),
                "Updated task description",
                true,
                null, // assignedUserId - можно оставить null или назначить другого пользователя
                testUser.getId()
        );

        assertEquals("Updated task description", updated.getDescription());
        assertTrue(updated.isCompleted());
    }

    @Test
    void updateTask_WithAssignedUser_ShouldUpdateAssignment() {
        // Тест обновления задачи с назначением пользователя
        Task task = taskService.createTask("Task to update", testEvent.getId(), testUser.getId(), null);
        User assignedUser = createTestUser("newassignee");
        eventService.addParticipant(testEvent.getId(), assignedUser.getId());

        Task updated = taskService.updateTask(
                task.getId(),
                "Updated description",
                false,
                assignedUser.getId(),
                testUser.getId()
        );

        assertEquals(assignedUser.getId(), updated.getAssignedUser().getId());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void updateTask_ShouldThrowWhenNotCreator() {
        // Тест проверки прав доступа при обновлении
        Task task = taskService.createTask("Test task", testEvent.getId(), testUser.getId(), null);

        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("password");
        User registeredOtherUser = userService.registerUser(otherUser);

        assertThrows(AccessDeniedException.class, () -> {
            taskService.updateTask(task.getId(), "Hacked description", false, null, registeredOtherUser.getId());
        });
    }

    @Test
    void deleteTask_ShouldRemoveTask() {
        // Тест удаления задачи
        Task task = taskService.createTask("Task to delete", testEvent.getId(), testUser.getId(), null);
        taskService.deleteTask(task.getId(), testUser.getId());

        var tasks = taskService.getTasksForEvent(testEvent.getId());
        assertFalse(tasks.stream().anyMatch(t -> t.getId().equals(task.getId())));
    }

    @Test
    void deleteTask_ShouldThrowWhenNotCreator() {
        // Тест проверки прав доступа при удалении
        Task task = taskService.createTask("Task to delete", testEvent.getId(), testUser.getId(), null);

        User otherUser = createTestUser("otheruser2");

        assertThrows(AccessDeniedException.class, () -> {
            taskService.deleteTask(task.getId(), otherUser.getId());
        });
    }

    @Test
    void isTaskCreator_ShouldReturnTrueForCreator() {
        // Тест проверки создателя задачи
        Task task = taskService.createTask("Test task", testEvent.getId(), testUser.getId(), null);

        boolean isCreator = taskService.isTaskCreator(task.getId(), testUser.getId());

        assertTrue(isCreator);
    }

    @Test
    void isTaskCreator_ShouldReturnFalseForNonCreator() {
        // Тест проверки создателя задачи для не-создателя
        Task task = taskService.createTask("Test task", testEvent.getId(), testUser.getId(), null);

        User otherUser = createTestUser("otheruser3");

        boolean isCreator = taskService.isTaskCreator(task.getId(), otherUser.getId());

        assertFalse(isCreator);
    }

    // Вспомогательный метод для создания тестового пользователя
    private User createTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        return userService.registerUser(user);
    }
}