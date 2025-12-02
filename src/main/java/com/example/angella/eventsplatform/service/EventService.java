package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.model.EventFilterModel;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.LocationRepository;
import com.example.angella.eventsplatform.repository.specification.EventSpecification;
import com.example.angella.eventsplatform.web.dto.UpdateEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final LocationRepository locationRepository;
    private final UserService userService;
    private final ImageService imageService;
    private final ChatService chatService;
    private final ApplicationEventPublisher eventPublisher;
    private final EventAccessService eventAccessService;
    private final TaskService taskService;
    private final ChecklistService checklistService;

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        List<Event> events = eventRepository.findAll();
        events.forEach(this::initializeLazyCollections);
        return events != null ? events : List.of(); // Защита от null
    }

    @Transactional(readOnly = true)
    public Page<Event> filter(EventFilterModel filterModel) {
        Page<Event> page = eventRepository.findAll(
                EventSpecification.withFilter(filterModel),
                filterModel.getPage().toPageRequest()
        );
        page.getContent().forEach(this::initializeLazyCollections);
        return page;
    }

    @Transactional(readOnly = true)
    public Event getById(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new EntityNotFoundException(
                        MessageFormat.format("Event with id {0} not found!", eventId)
                ));
        initializeLazyCollections(event);

        List<Image> images = imageService.getEventImages(eventId);
        event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());

        return event;
    }

    @Transactional(readOnly = true)
    public Event getByIdWithRelations(Long id) {
        Event event = eventRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));
        initializeLazyCollections(event);

        List<Image> images = imageService.getEventImages(id);
        event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());

        return event;
    }

    @Transactional(readOnly = true)
    public Event getEventForDetailView(Long eventId) {
        try {
            Event event = eventRepository.findByIdWithRelations(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            List<Image> eventImages = imageService.getEventImages(eventId);
            event.setImages(eventImages != null ? new HashSet<>(eventImages) : new HashSet<>());

            return event;
        } catch (Exception e) {
            log.error("Error loading event for detail view: {}", eventId, e);
            throw new RuntimeException("Failed to load event details", e);
        }
    }

    @Transactional
    public Event create(Event event, Long creatorId) {
        try {
            event.setCategories(categoryService.upsertCategories(event.getCategories()));
            var location = locationRepository.findByCityAndStreet(
                            event.getLocation().getCity(),
                            event.getLocation().getStreet())
                    .orElseGet(() -> locationRepository.save(event.getLocation()));
            event.setLocation(location);
            User creator = userService.findById(creatorId);
            event.setCreator(creator);
            event.addParticipant(creator);
            Event savedEvent = eventRepository.save(event);

            // Создание дополнительных сущностей с обработкой ошибок
            createEventAdditionalEntities(savedEvent, creatorId);

            initializeLazyCollections(savedEvent);
            return savedEvent;
        } catch (Exception e) {
            log.error("Failed to create event", e);
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    private void createEventAdditionalEntities(Event event, Long creatorId) {
        try {
            chatService.createMessage("Добро пожаловать в чат мероприятия!", event.getId(), creatorId);
            log.info("Created welcome message for event chat: {}", event.getId());
        } catch (Exception e) {
            log.warn("Failed to create welcome message in chat: {}", e.getMessage());
        }

        try {
            taskService.createTask("Организовать мероприятие", event.getId(), creatorId, null);
            log.info("Created default task for event: {}", event.getId());
        } catch (Exception e) {
            log.warn("Failed to create default task: {}", e.getMessage());
        }

        try {
            checklistService.createItem("Подготовить место проведения",
                    "Организовать пространство для мероприятия", 1, event.getId(), creatorId, null);
            log.info("Created default checklist items for event: {}", event.getId());
        } catch (Exception e) {
            log.warn("Failed to create default checklist items: {}", e.getMessage());
        }
    }

    @Transactional
    public Event updateEvent(Long eventId, UpdateEventRequest request, Long currentUserId) {
        Event existingEvent = getByIdWithRelations(eventId);

        if (!eventAccessService.isEventCreator(eventId, currentUserId)) {
            throw new AccessDeniedException("Only event creator can update the event");
        }

        if (StringUtils.isNotBlank(request.getName())) {
            existingEvent.setName(request.getName());
        }
        if (request.getStartTime() != null) {
            existingEvent.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            existingEvent.setEndTime(request.getEndTime());
        }

        if (!CollectionUtils.isEmpty(request.getCategories())) {
            Set<Category> updatedCategories = request.getCategories().stream()
                    .map(categoryName -> new Category(null, categoryName, null))
                    .collect(Collectors.toSet());
            existingEvent.setCategories(categoryService.upsertCategories(updatedCategories));
        }

        if (StringUtils.isNotBlank(request.getCity()) || StringUtils.isNotBlank(request.getStreet())) {
            Location location = locationRepository.findByCityAndStreet(
                    request.getCity() != null ? request.getCity() : existingEvent.getLocation().getCity(),
                    request.getStreet() != null ? request.getStreet() : existingEvent.getLocation().getStreet()
            ).orElseGet(() -> {
                Location newLocation = new Location();
                newLocation.setCity(request.getCity());
                newLocation.setStreet(request.getStreet());
                return locationRepository.save(newLocation);
            });
            existingEvent.setLocation(location);
        }

        if (StringUtils.isNotBlank(request.getDescription())) {
            existingEvent.setDescription(request.getDescription());
        }

        Event updatedEvent = eventRepository.save(existingEvent);
        initializeLazyCollections(updatedEvent);
        return updatedEvent;
    }

    @Transactional
    public boolean addParticipant(Long eventId, Long participantId) {
        Event event = getById(eventId);
        User participant = userService.findById(participantId);
        boolean isAdded = event.addParticipant(participant);
        if (isAdded) {
            eventRepository.save(event);
        }
        return isAdded;
    }

    @Transactional
    public boolean removeParticipant(Long eventId, Long participantId) {
        Event event = getById(eventId);
        User participant = userService.findById(participantId);

        if (!event.getParticipants().contains(participant)) {
            return false;
        }

        boolean isRemoved = event.removeParticipant(participant);
        if (isRemoved) {
            eventRepository.save(event);
        }
        return isRemoved;
    }

    @Transactional
    public void deleteById(Long id, Long currentUserId) {
        Event event = getById(id);
        if (!eventAccessService.isEventCreator(id, currentUserId)) {
            throw new AccessDeniedException("Only event creator can delete the event");
        }
        eventRepository.deleteById(id);
    }

    public boolean hasParticipant(Long eventId, Long participantId) {
        return eventAccessService.hasParticipant(eventId, participantId);
    }

    public boolean isEventCreator(Long eventId, Long userId) {
        return eventAccessService.isEventCreator(eventId, userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEventByAdmin(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }
        eventRepository.deleteById(eventId);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        return eventRepository.findAllDistinctCities();
    }

    @Transactional(readOnly = true)
    public List<String> searchCities(String search) {
        if (search == null || search.trim().isEmpty()) {
            return getAllCities();
        }
        return eventRepository.findDistinctCitiesBySearch(search.trim());
    }

    @Transactional(readOnly = true)
    public List<Image> getEventImages(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }
        return imageService.getEventImages(eventId);
    }

    @Transactional(readOnly = true)
    public Image getMainEventImage(Long eventId) {
        List<Image> images = imageService.getEventImages(eventId);
        return images.isEmpty() ? null : images.get(0);
    }

    @Transactional(readOnly = true)
    public List<Event> findAllWithImages() {
        List<Event> events = eventRepository.findAll();
        events.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });
        return events;
    }

    @Transactional(readOnly = true)
    public List<Event> findFeaturedEvents() {
        List<Event> events = eventRepository.findAllOrderByStartTimeDesc();

        Instant now = Instant.now();
        List<Event> futureEvents = events.stream()
                .filter(event -> event.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        futureEvents.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });

        return futureEvents.size() > 6 ? futureEvents.subList(0, 6) : futureEvents;
    }

    @Transactional(readOnly = true)
    public List<Event> findUpcomingEvents() {
        List<Event> events = eventRepository.findAllOrderByStartTimeAsc();

        Instant now = Instant.now();
        List<Event> futureEvents = events.stream()
                .filter(event -> event.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        futureEvents.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });

        return futureEvents.size() > 6 ? futureEvents.subList(0, 6) : futureEvents;
    }

    @Transactional(readOnly = true)
    public List<Event> findUserEventsWithImages(Long userId) {
        User user = userService.findById(userId);

        List<Event> allEvents = eventRepository.findAll();
        Instant now = Instant.now();

        List<Event> userEvents = allEvents.stream()
                .filter(event -> event.getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(userId)))
                .filter(event -> event.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        userEvents.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });

        return userEvents;
    }

    @Transactional(readOnly = true)
    public List<Event> findAllFutureEvents() {
        List<Event> events = eventRepository.findAll();

        Instant now = Instant.now();
        List<Event> futureEvents = events.stream()
                .filter(event -> event.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        futureEvents.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });

        return futureEvents;
    }

    public String getEventStatus(Event event) {
        Instant now = Instant.now();
        if (event.getStartTime().isAfter(now)) {
            return "UPCOMING";
        } else if (event.getStartTime().isBefore(now) && event.getEndTime().isAfter(now)) {
            return "ACTIVE";
        } else {
            return "COMPLETED";
        }
    }

    private void initializeLazyCollections(Event event) {
        if (event.getParticipants() != null) {
            event.getParticipants().size();
        }
        if (event.getCategories() != null) {
            event.getCategories().size();
        }
        if (event.getComments() != null) {
            event.getComments().size();
        }
        if (event.getChatMessages() != null) {
            event.getChatMessages().size();
        }
        if (event.getTasks() != null) {
            event.getTasks().size();
        }
    }

    @Transactional(readOnly = true)
    public List<Event> findAllUserEvents(Long userId) {
        List<Event> allEvents = eventRepository.findAll();

        List<Event> userEvents = allEvents.stream()
                .filter(event -> event.getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(userId)))
                .collect(Collectors.toList());

        userEvents.forEach(event -> {
            initializeLazyCollections(event);
            List<Image> images = imageService.getEventImages(event.getId());
            event.setImages(images != null ? new HashSet<>(images) : new HashSet<>());
        });

        return userEvents;
    }

    public Long getTotalEventsCount() {
        return eventRepository.count();
    }

    public Long getUpcomingEventsCount() {
        Instant now = Instant.now();
        return eventRepository.countByStartTimeAfter(now);
    }

    public Integer getAverageParticipantsPerEvent() {
        List<Event> events = eventRepository.findAllWithParticipantsCount();
        if (events.isEmpty()) {
            return 0;
        }

        int totalParticipants = events.stream()
                .mapToInt(event -> event.getParticipants().size())
                .sum();

        return totalParticipants / events.size(); // Целочисленное среднее
    }

    public List<Object[]> getMostPopularCategories(int limit) {
        return eventRepository.findMostPopularCategories(limit);
    }
}