package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.web.dto.UpdateEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceIT extends ServiceIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private UserService userService;
    @Autowired
    private CategoryService categoryService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Инициализация тестового пользователя и категории перед каждым тестом
        testUser = new User();
        testUser.setUsername("eventcreator");
        testUser.setEmail("creator@example.com");
        testUser.setPassword("password");
        testUser = userService.registerUser(testUser);

        testCategory = new Category();
        testCategory.setName("TestCategory");
        testCategory = categoryService.upsertCategories(Set.of(testCategory)).iterator().next();
    }

    @Test
    void createEvent_ShouldSaveWithAllRelations() {
        // Создание тестового события
        Event event = buildTestEvent();
        // Вызов метода создания события
        Event savedEvent = eventService.create(event, testUser.getId());

        // Проверки:
        // - что событие было сохранено (имеет ID)
        assertNotNull(savedEvent.getId());
        // - что название события корректно
        assertEquals("Test Event", savedEvent.getName());
        // - что описание события корректно
        assertEquals("Test Event Description", savedEvent.getDescription());
        // - что создатель события соответствует тестовому пользователю
        assertEquals(testUser.getId(), savedEvent.getCreator().getId());
        // - что событие имеет одну категорию
        assertEquals(1, savedEvent.getCategories().size());
    }

    @Test
    void updateEvent_ShouldUpdateDescription() {
        // Создание тестового события
        Event event = createTestEvent();

        // Подготовка запроса на обновление
        UpdateEventRequest updateRequest = new UpdateEventRequest();
        updateRequest.setDescription("Updated Event Description");

        // Обновление события
        Event updatedEvent = eventService.updateEvent(event.getId(), updateRequest, testUser.getId());

        // Проверка, что описание обновилось
        assertEquals("Updated Event Description", updatedEvent.getDescription());
    }

    @Test
    void addParticipant_ShouldAddUserToEvent() {
        // Создание тестового события
        Event event = createTestEvent();
        // Создание пользователя-участника
        User participant = createTestUser("participant");

        // Добавление участника к событию
        boolean result = eventService.addParticipant(event.getId(), participant.getId());

        // Проверки:
        // - что метод добавления вернул true (успех)
        assertTrue(result);
        // - что событие теперь содержит этого участника
        assertTrue(eventService.hasParticipant(event.getId(), participant.getId()));
    }

    @Test
    void updateEvent_ShouldThrowWhenNotCreator() {
        // Создание тестового события
        Event event = createTestEvent();
        // Создание другого пользователя (не создателя события)
        User anotherUser = createTestUser("anotheruser");

        // Подготовка запроса на обновление
        UpdateEventRequest updateRequest = new UpdateEventRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated Description");

        // Проверка, что при попытке обновления не создателем
        // будет выброшено исключение AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                eventService.updateEvent(event.getId(), updateRequest, anotherUser.getId()));
    }

    @Test
    void createEvent_ShouldPersistDescription() {
        // Создание события с описанием
        Event event = buildTestEvent();
        event.setDescription("Detailed event description with schedule information");

        Event savedEvent = eventService.create(event, testUser.getId());

        // Проверка, что описание сохранилось
        assertNotNull(savedEvent.getDescription());
        assertEquals("Detailed event description with schedule information", savedEvent.getDescription());
    }

    // Вспомогательный метод для создания тестового события (уже сохраненного в БД)
    private Event createTestEvent() {
        Event event = buildTestEvent();
        return eventService.create(event, testUser.getId());
    }

    // Вспомогательный метод для построения объекта события (без сохранения в БД)
    private Event buildTestEvent() {
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("Test Event Description"); // ДОБАВЛЕНО поле description
        event.setStartTime(Instant.now().plusSeconds(3600));
        event.setEndTime(Instant.now().plusSeconds(7200));
        event.setCategories(Set.of(testCategory));

        Location location = new Location();
        location.setCity("Test City");
        location.setStreet("Test Street");
        event.setLocation(location);

        event.setCreator(testUser);
        return event;
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