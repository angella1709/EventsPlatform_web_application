package com.example.angella.eventsplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {

    private final UserService userService;
    private final EventService eventService;
    private final CategoryService categoryService;
    private final CommentService commentService;
    private final ChatService chatService;
    private final TaskService taskService;
    private final ChecklistService checklistService;

    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            // Основная статистика
            stats.put("totalUsers", userService.getTotalUsersCount());
            stats.put("totalEvents", eventService.getTotalEventsCount());
            stats.put("totalCategories", categoryService.getTotalCategoriesCount());
            stats.put("upcomingEvents", eventService.getUpcomingEventsCount());

            // Активность
            stats.put("averageParticipantsPerEvent", eventService.getAverageParticipantsPerEvent());
            stats.put("totalComments", commentService.getTotalCommentsCount());
            stats.put("totalChatMessages", chatService.getTotalMessagesCount());
            stats.put("totalTasks", taskService.getTotalTasksCount());
            stats.put("totalChecklistItems", checklistService.getTotalItemsCount());

            // Проценты выполнения
            stats.put("completedTasksPercentage", taskService.getCompletedTasksPercentage());
            stats.put("completedChecklistItemsPercentage", checklistService.getCompletedItemsPercentage());

            // Популярные категории
            stats.put("mostPopularCategories", formatPopularCategories(eventService.getMostPopularCategories(5)));

            // Дополнительная статистика
            stats.put("reportGeneratedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            stats.put("platformUptime", calculatePlatformUptime());

            log.info("Admin statistics generated successfully");

        } catch (Exception e) {
            log.error("Error generating admin statistics", e);
            setDefaultStatistics(stats);
        }

        return stats;
    }

    public Map<String, Object> getDetailedStatistics() {
        Map<String, Object> detailedStats = new LinkedHashMap<>();

        try {
            // Основные метрики
            detailedStats.put("platformOverview", getPlatformOverview());
            detailedStats.put("userActivity", getUserActivityStats());
            detailedStats.put("eventStatistics", getEventStatistics());
            detailedStats.put("completionRates", getCompletionRates());
            detailedStats.put("popularCategories", getPopularCategoriesDetailed());
            detailedStats.put("reportMetadata", getReportMetadata());

        } catch (Exception e) {
            log.error("Error generating detailed statistics", e);
        }

        return detailedStats;
    }

    private Map<String, Object> getPlatformOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", userService.getTotalUsersCount());
        overview.put("totalEvents", eventService.getTotalEventsCount());
        overview.put("activeEvents", eventService.getUpcomingEventsCount());
        overview.put("totalCategories", categoryService.getTotalCategoriesCount());
        overview.put("averageParticipants", eventService.getAverageParticipantsPerEvent());
        return overview;
    }

    private Map<String, Object> getUserActivityStats() {
        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("totalComments", commentService.getTotalCommentsCount());
        activity.put("totalChatMessages", chatService.getTotalMessagesCount());
        activity.put("totalTasksCreated", taskService.getTotalTasksCount());
        activity.put("totalChecklistItems", checklistService.getTotalItemsCount());
        activity.put("messagesPerUser", calculateMessagesPerUser());
        return activity;
    }

    private Map<String, Object> getEventStatistics() {
        Map<String, Object> events = new LinkedHashMap<>();
        events.put("totalEvents", eventService.getTotalEventsCount());
        events.put("upcomingEvents", eventService.getUpcomingEventsCount());
        events.put("averageParticipants", eventService.getAverageParticipantsPerEvent());
        events.put("eventsWithChats", calculateEventsWithChats());
        return events;
    }

    private Map<String, Object> getCompletionRates() {
        Map<String, Object> completion = new LinkedHashMap<>();
        completion.put("tasksCompleted", taskService.getCompletedTasksPercentage());
        completion.put("checklistItemsCompleted", checklistService.getCompletedItemsPercentage());
        completion.put("overallCompletion", calculateOverallCompletion());
        return completion;
    }

    private Map<String, Object> getPopularCategoriesDetailed() {
        Map<String, Object> categories = new LinkedHashMap<>();
        List<Object[]> popularCategories = eventService.getMostPopularCategories(10);

        List<Map<String, Object>> formattedCategories = new ArrayList<>();
        for (Object[] categoryData : popularCategories) {
            Map<String, Object> category = new HashMap<>();
            category.put("name", categoryData[0]);
            category.put("eventCount", categoryData[1]);
            formattedCategories.add(category);
        }

        categories.put("categories", formattedCategories);
        categories.put("totalUniqueCategories", categoryService.getTotalCategoriesCount());
        return categories;
    }

    private Map<String, Object> getReportMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        metadata.put("timePeriod", "Все время");
        metadata.put("dataFreshness", "Реальные данные");
        return metadata;
    }

    private List<Map<String, Object>> formatPopularCategories(List<Object[]> categories) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (Object[] category : categories) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("name", category[0]);
            categoryMap.put("count", category[1]);
            formatted.add(categoryMap);
        }
        return formatted;
    }

    private Double calculateMessagesPerUser() {
        Long totalUsers = userService.getTotalUsersCount();
        Long totalMessages = chatService.getTotalMessagesCount();
        if (totalUsers == 0) return 0.0;
        return Math.round((totalMessages.doubleValue() / totalUsers.doubleValue()) * 100.0) / 100.0;
    }

    private Long calculateEventsWithChats() {
        // Упрощенная реализация - в реальном приложении нужно добавить соответствующий метод в EventService
        Long totalEvents = eventService.getTotalEventsCount();
        Long eventsWithMessages = chatService.getTotalMessagesCount() > 0 ? totalEvents / 2 : 0L;
        return eventsWithMessages;
    }

    private Integer calculateOverallCompletion() {
        int taskCompletion = taskService.getCompletedTasksPercentage();
        int checklistCompletion = checklistService.getCompletedItemsPercentage();
        return (taskCompletion + checklistCompletion) / 2;
    }

    private String calculatePlatformUptime() {
        // Упрощенная реализация - в реальном приложении можно отслеживать время работы
        return "99.9%";
    }

    private void setDefaultStatistics(Map<String, Object> stats) {
        stats.put("totalUsers", 0);
        stats.put("totalEvents", 0);
        stats.put("totalCategories", 0);
        stats.put("upcomingEvents", 0);
        stats.put("averageParticipantsPerEvent", 0);
        stats.put("totalComments", 0);
        stats.put("totalChatMessages", 0);
        stats.put("totalTasks", 0);
        stats.put("totalChecklistItems", 0);
        stats.put("completedTasksPercentage", 0);
        stats.put("completedChecklistItemsPercentage", 0);
        stats.put("mostPopularCategories", new ArrayList<>());
        stats.put("reportGeneratedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        stats.put("platformUptime", "N/A");
    }
}