package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.ChecklistItemRepository;
import com.example.angella.eventsplatform.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChecklistServiceIT extends ServiceIntegrationTest {

    @Autowired private ChecklistService checklistService;
    @Autowired private EventService eventService;
    @Autowired private UserService userService;
    @Autowired private ChecklistItemRepository checklistItemRepository;
    @Autowired private LocationRepository locationRepository;

    private User organizer;
    private User participant1;
    private User participant2;
    private User nonParticipant;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Создаем пользователей
        organizer = createUser("organizer", "organizer@test.com");
        participant1 = createUser("participant1", "participant1@test.com");
        participant2 = createUser("participant2", "participant2@test.com");
        nonParticipant = createUser("nonparticipant", "nonparticipant@test.com");

        // Создаем тестовое мероприятие
        testEvent = createTestEvent(organizer);
        eventService.addParticipant(testEvent.getId(), participant1.getId());
        eventService.addParticipant(testEvent.getId(), participant2.getId());
    }

    @Test
    void createChecklistItem_ShouldCreateItemSuccessfully() {
        // Arrange & Act
        ChecklistItem item = checklistService.createItem(
                "Test Item",
                "Test Description",
                2,
                testEvent.getId(),
                organizer.getId(),
                null
        );

        // Assert
        assertNotNull(item.getId());
        assertEquals("Test Item", item.getName());
        assertEquals("Test Description", item.getDescription());
        assertEquals(2, item.getQuantity());
        assertFalse(item.isCompleted());
        assertEquals(organizer.getId(), item.getCreatedBy().getId());
        assertNull(item.getAssignedUser());
    }

    @Test
    void createChecklistItem_WithAssignedUser_ShouldAssignSuccessfully() {
        // Act
        ChecklistItem item = checklistService.createItem(
                "Assigned Item",
                null,
                1,
                testEvent.getId(),
                organizer.getId(),
                participant1.getId()
        );

        // Assert
        assertNotNull(item.getAssignedUser());
        assertEquals(participant1.getId(), item.getAssignedUser().getId());
    }

    @Test
    void createChecklistItem_ByNonParticipant_ShouldThrowAccessDenied() {
        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                checklistService.createItem(
                        "Test Item", null, 1, testEvent.getId(),
                        nonParticipant.getId(), null
                )
        );
    }

    @Test
    void createChecklistItem_WithNonParticipantAssignee_ShouldThrowAccessDenied() {
        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                checklistService.createItem(
                        "Test Item", null, 1, testEvent.getId(),
                        organizer.getId(), nonParticipant.getId()
                )
        );
    }

    @Test
    void getChecklistForEvent_ShouldReturnAllItems() {
        // Arrange
        checklistService.createItem("Item 1", null, 1, testEvent.getId(), organizer.getId(), null);
        checklistService.createItem("Item 2", null, 1, testEvent.getId(), participant1.getId(), null);

        // Act
        List<ChecklistItem> items = checklistService.getChecklistForEvent(testEvent.getId());

        // Assert
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(item -> "Item 1".equals(item.getName())));
        assertTrue(items.stream().anyMatch(item -> "Item 2".equals(item.getName())));
    }

    @Test
    void toggleItemCompletion_ShouldToggleStatus() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "Toggle Item", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act & Assert - первый переключение
        ChecklistItem toggled = checklistService.toggleItemCompletion(item.getId(), participant1.getId());
        assertTrue(toggled.isCompleted());

        // Act & Assert - второе переключение
        ChecklistItem untoggled = checklistService.toggleItemCompletion(item.getId(), participant2.getId());
        assertFalse(untoggled.isCompleted());
    }

    @Test
    void toggleItemCompletion_ByNonParticipant_ShouldThrowAccessDenied() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "Toggle Item", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                checklistService.toggleItemCompletion(item.getId(), nonParticipant.getId())
        );
    }

    @Test
    void updateChecklistItem_ShouldUpdateFields() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "Original", "Original Desc", 1, testEvent.getId(), organizer.getId(), null
        );

        // Act
        ChecklistItem updated = checklistService.updateItem(
                item.getId(), "Updated", "Updated Desc", 3, true, participant2.getId(), organizer.getId()
        );

        // Assert
        assertEquals("Updated", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(3, updated.getQuantity());
        assertTrue(updated.isCompleted());
        assertEquals(participant2.getId(), updated.getAssignedUser().getId());
    }

    @Test
    void updateChecklistItem_ByNonCreator_ShouldThrowAccessDenied() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "Test Item", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                checklistService.updateItem(
                        item.getId(), "Hacked", null, null, null, null, participant1.getId()
                )
        );
    }

    @Test
    void deleteChecklistItem_ShouldRemoveItem() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "To Delete", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act
        checklistService.deleteItem(item.getId(), organizer.getId());

        // Assert
        List<ChecklistItem> items = checklistService.getChecklistForEvent(testEvent.getId());
        assertTrue(items.stream().noneMatch(i -> i.getId().equals(item.getId())));
    }

    @Test
    void deleteChecklistItem_ByNonCreator_ShouldThrowAccessDenied() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "To Delete", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                checklistService.deleteItem(item.getId(), participant1.getId())
        );
    }

    @Test
    void checklistStatistics_ShouldCalculateCorrectly() {
        // Arrange
        checklistService.createItem("Item 1", null, 1, testEvent.getId(), organizer.getId(), null);
        checklistService.createItem("Item 2", null, 1, testEvent.getId(), organizer.getId(), null);
        checklistService.createItem("Item 3", null, 1, testEvent.getId(), organizer.getId(), null);

        List<ChecklistItem> items = checklistService.getChecklistForEvent(testEvent.getId());

        // Отмечаем один элемент как выполненный
        checklistService.toggleItemCompletion(items.get(0).getId(), organizer.getId());

        // Act & Assert - используем репозиторий напрямую для статистики
        long completed = checklistItemRepository.countCompletedItems(testEvent.getId());
        long total = checklistItemRepository.countTotalItems(testEvent.getId());

        assertEquals(1, completed);
        assertEquals(3, total);
    }

    @Test
    void isItemCreator_ShouldReturnCorrectStatus() {
        // Arrange
        ChecklistItem item = checklistService.createItem(
                "Test Item", null, 1, testEvent.getId(), organizer.getId(), null
        );

        // Act & Assert
        assertTrue(checklistService.isItemCreator(item.getId(), organizer.getId()));
        assertFalse(checklistService.isItemCreator(item.getId(), participant1.getId()));
    }

    // Вспомогательные методы
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        return userService.registerUser(user);
    }

    private Event createTestEvent(User creator) {
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("Test Event Description");
        event.setStartTime(Instant.now().plusSeconds(3600));
        event.setEndTime(Instant.now().plusSeconds(7200));

        Location location = new Location();
        location.setCity("Test City");
        location.setStreet("Test Street");
        location = locationRepository.save(location);
        event.setLocation(location);

        event.setCreator(creator);
        return eventService.create(event, creator.getId());
    }
}