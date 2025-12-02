package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChatServiceIT extends ServiceIntegrationTest {

    // Сервисы и репозитории для тестирования чата
    @Autowired
    private ChatService chatService;
    @Autowired
    private EventService eventService;
    @Autowired
    private UserService userService;
    @Autowired
    private LocationRepository locationRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Инициализация тестового пользователя
        testUser = new User();
        testUser.setUsername("chatuser");
        testUser.setEmail("chat@example.com");
        testUser.setPassword("password");
        testUser = userService.registerUser(testUser);

        // Создание тестового события
        testEvent = buildTestEvent();
        testEvent = eventService.create(testEvent, testUser.getId());
    }

    private Event buildTestEvent() {
        Event event = new Event();
        event.setName("Chat Test Event");
        event.setDescription("Test event for chat functionality");
        event.setStartTime(Instant.now());
        event.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));

        // Настройка локации события
        Location location = new Location();
        location.setCity("Test City");
        location.setStreet("Test Street");
        location = locationRepository.save(location);
        event.setLocation(location);

        event.setCreator(testUser);
        return event;
    }

    @Test
    void createMessage_ShouldSaveMessage() {
        // Тест создания сообщения в чате
        ChatMessage message = chatService.createMessage(
                "Test message",
                testEvent.getId(),
                testUser.getId()
        );

        assertNotNull(message.getId());
        assertEquals("Test message", message.getContent());
        assertEquals(testEvent.getId(), message.getEvent().getId());
        assertEquals(testUser.getId(), message.getAuthor().getId());
    }

    @Test
    void getMessages_ShouldReturnPaginatedMessages() {
        // Тест получения сообщений с пагинацией
        chatService.createMessage("Message 1", testEvent.getId(), testUser.getId());
        chatService.createMessage("Message 2", testEvent.getId(), testUser.getId());

        Page<ChatMessage> messages = chatService.getMessages(
                testEvent.getId(),
                new PageModel(0, 10)
        );

        assertEquals(3, messages.getTotalElements());
        assertEquals(3, messages.getContent().size());
    }

    @Test
    void getMessages_ShouldReturnEmptyPageForNonExistentEvent() {
        // Тест получения сообщений для несуществующего события
        assertThrows(EntityNotFoundException.class, () -> {
            chatService.getMessages(
                    999L, // Несуществующий ID
                    new PageModel(0, 10)
            );
        });
    }

    @Test
    void updateMessage_ShouldChangeContent() {
        // Тест обновления сообщения
        ChatMessage message = chatService.createMessage(
                "Original message",
                testEvent.getId(),
                testUser.getId()
        );

        ChatMessage updated = chatService.updateMessage(
                message.getId(),
                "Updated message content",
                testUser.getId()
        );

        assertEquals("Updated message content", updated.getContent());
        assertTrue(updated.isEdited());
    }

    @Test
    void updateMessage_ShouldThrowWhenNotAuthor() {
        // Тест проверки прав на редактирование
        ChatMessage message = chatService.createMessage(
                "Test message",
                testEvent.getId(),
                testUser.getId()
        );

        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("password");
        User registeredOtherUser = userService.registerUser(otherUser);

        assertThrows(AccessDeniedException.class, () -> {
            chatService.updateMessage(message.getId(), "Hacked message", registeredOtherUser.getId());
        });
    }

    @Test
    void deleteMessage_ShouldRemoveMessage() {
        // Тест удаления сообщения
        ChatMessage message = chatService.createMessage(
                "Message to delete",
                testEvent.getId(),
                testUser.getId()
        );

        // Удаление сообщения
        chatService.deleteMessage(message.getId(), testUser.getId());

        // Проверка, что сообщение удалено
        Page<ChatMessage> messages = chatService.getMessages(
                testEvent.getId(),
                new PageModel(0, 10)
        );

        assertFalse(messages.getContent().stream()
                .anyMatch(m -> m.getId().equals(message.getId())));
    }

    @Test
    void deleteMessage_ShouldThrowWhenNotAuthor() {
        // Тест проверки прав на удаление
        ChatMessage message = chatService.createMessage(
                "Message to delete",
                testEvent.getId(),
                testUser.getId()
        );

        User otherUser = new User();
        otherUser.setUsername("otheruser2");
        otherUser.setEmail("other2@test.com");
        otherUser.setPassword("password");
        User registeredOtherUser = userService.registerUser(otherUser);

        assertThrows(AccessDeniedException.class, () -> {
            chatService.deleteMessage(message.getId(), registeredOtherUser.getId());
        });
    }

    @Test
    void isMessageAuthor_ShouldReturnTrueForAuthor() {
        // Тест проверки авторства сообщения
        ChatMessage message = chatService.createMessage(
                "Test message",
                testEvent.getId(),
                testUser.getId()
        );

        boolean isAuthor = chatService.isMessageAuthor(message.getId(), testUser.getId());

        assertTrue(isAuthor);
    }

    @Test
    void isMessageAuthor_ShouldReturnFalseForNonAuthor() {
        // Тест проверки авторства для не-автора
        ChatMessage message = chatService.createMessage(
                "Test message",
                testEvent.getId(),
                testUser.getId()
        );

        User otherUser = new User();
        otherUser.setUsername("otheruser3");
        otherUser.setEmail("other3@test.com");
        otherUser.setPassword("password");
        User registeredOtherUser = userService.registerUser(otherUser);

        boolean isAuthor = chatService.isMessageAuthor(message.getId(), registeredOtherUser.getId());

        assertFalse(isAuthor);
    }
}