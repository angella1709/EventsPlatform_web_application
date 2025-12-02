package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.service.*;
import com.example.angella.eventsplatform.web.dto.ChecklistTemplateRequest;
import com.example.angella.eventsplatform.web.dto.TemplateItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final EventService eventService;
    private final ChecklistTemplateService templateService;
    private final StatisticsService statisticsService;
    private final PdfReportService pdfReportService;
    private final ChecklistTemplateService checklistTemplateService;

    @GetMapping("/users")
    public String userManagement(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении пользователя: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/toggle-admin")
    public String toggleAdminRole(@PathVariable Long userId,
                                  @RequestParam boolean makeAdmin,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (makeAdmin) {
                userService.addAdminRole(userId);
                redirectAttributes.addFlashAttribute("success", "Права администратора добавлены");
            } else {
                userService.removeAdminRole(userId);
                redirectAttributes.addFlashAttribute("success", "Права администратора удалены");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при изменении прав: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/events")
    public String eventManagement(Model model) {
        try {
            List<Event> allEvents = eventService.findAll();

            // Безопасный расчет статистики
            long totalParticipants = allEvents.stream()
                    .mapToLong(event -> event.getParticipants() != null ? event.getParticipants().size() : 0)
                    .sum();

            Instant now = Instant.now();
            long activeEvents = allEvents.stream()
                    .filter(event -> {
                        if (event.getStartTime() == null || event.getEndTime() == null) return false;
                        return event.getStartTime().isBefore(now) && event.getEndTime().isAfter(now);
                    })
                    .count();

            long upcomingEvents = allEvents.stream()
                    .filter(event -> event.getStartTime() != null && event.getStartTime().isAfter(now))
                    .count();

            long completedEvents = allEvents.stream()
                    .filter(event -> event.getEndTime() != null && event.getEndTime().isBefore(now))
                    .count();

            // Передаем безопасные значения
            model.addAttribute("events", allEvents != null ? allEvents : List.of());
            model.addAttribute("totalEvents", allEvents != null ? allEvents.size() : 0);
            model.addAttribute("totalParticipants", totalParticipants);
            model.addAttribute("activeEvents", activeEvents);
            model.addAttribute("upcomingEvents", upcomingEvents);
            model.addAttribute("completedEvents", completedEvents);

            return "admin/events";

        } catch (Exception e) {
            log.error("Error in event management", e);
            // Значения по умолчанию при ошибке
            model.addAttribute("error", "Ошибка загрузки мероприятий: " + e.getMessage());
            model.addAttribute("events", List.of());
            model.addAttribute("totalEvents", 0);
            model.addAttribute("totalParticipants", 0);
            model.addAttribute("activeEvents", 0);
            model.addAttribute("upcomingEvents", 0);
            model.addAttribute("completedEvents", 0);
            return "admin/events";
        }
    }

    @PostMapping("/events/{eventId}/delete")
    public String deleteEvent(@PathVariable Long eventId, RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEventByAdmin(eventId);
            redirectAttributes.addFlashAttribute("success", "Мероприятие успешно удалено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении мероприятия: " + e.getMessage());
        }
        return "redirect:/admin/events";
    }

    @GetMapping("/templates")
    public String templateManagement(Model model) {
        try {
            List<ChecklistTemplate> templates = templateService.getAllTemplates();

            // логирование для отладки
            log.info("Found {} templates in database", templates.size());
            templates.forEach(template ->
                    log.info("Template: id={}, name={}, itemsCount={}",
                            template.getId(), template.getName(),
                            template.getItems() != null ? template.getItems().size() : 0)
            );

            // УПРОЩЕННАЯ СТАТИСТИКА - убираем сложные вычисления
            long totalItems = templates.stream()
                    .mapToLong(t -> t.getItems() != null ? t.getItems().size() : 0)
                    .sum();

            long categoriesCount = templates.stream()
                    .map(ChecklistTemplate::getCategory)
                    .distinct()
                    .count();

            model.addAttribute("templates", templates);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("categoriesCount", categoriesCount);

            return "admin/templates";
        } catch (Exception e) {
            log.error("Error in template management", e);
            model.addAttribute("error", "Ошибка загрузки шаблонов: " + e.getMessage());
            model.addAttribute("templates", List.of());
            model.addAttribute("totalItems", 0);
            model.addAttribute("categoriesCount", 0);
            return "admin/templates";
        }
    }

    @GetMapping("/templates/create")
    public String createTemplateForm(Model model) {
        try {
            model.addAttribute("templateRequest", new ChecklistTemplateRequest());
            model.addAttribute("categories", TemplateCategory.values());
            model.addAttribute("isEdit", false); // Флаг для определения режима
            return "admin/template-form";
        } catch (Exception e) {
            log.error("Error loading template creation form", e);
            return "redirect:/admin/templates?error=load_failed";
        }
    }

    @PostMapping("/templates/create")
    public String createTemplate(@ModelAttribute ChecklistTemplateRequest templateRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Преобразуем DTO в Entity
            ChecklistTemplate template = new ChecklistTemplate();
            template.setName(templateRequest.getName());
            template.setDescription(templateRequest.getDescription());
            template.setCategory(templateRequest.getCategory());

            // Инициализируем коллекцию items
            template.setItems(new java.util.HashSet<>());

            // Сохраняем шаблон сначала без элементов
            ChecklistTemplate savedTemplate = templateService.createTemplate(template);

            // Добавляем элементы если они есть
            if (templateRequest.getItems() != null && !templateRequest.getItems().isEmpty()) {
                Set<TemplateItem> items = templateRequest.getItems().stream()
                        .map(itemRequest -> {
                            TemplateItem item = new TemplateItem();
                            item.setName(itemRequest.getName());
                            item.setDescription(itemRequest.getDescription());
                            item.setDefaultQuantity(itemRequest.getDefaultQuantity());
                            item.setTemplate(savedTemplate);
                            return item;
                        })
                        .collect(Collectors.toSet());

                savedTemplate.setItems(items);
                templateService.updateTemplate(savedTemplate.getId(), savedTemplate);
            }

            redirectAttributes.addFlashAttribute("success", "Шаблон успешно создан");
        } catch (Exception e) {
            log.error("Error creating template", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании шаблона: " + e.getMessage());
        }
        return "redirect:/admin/templates";
    }

    @GetMapping("/templates/{id}/edit")
    public String editTemplateForm(@PathVariable Long id, Model model) {
        try {
            ChecklistTemplate template = templateService.getTemplateById(id);

            ChecklistTemplateRequest templateRequest = new ChecklistTemplateRequest();
            templateRequest.setName(template.getName());
            templateRequest.setDescription(template.getDescription());
            templateRequest.setCategory(template.getCategory());

            // Безопасная обработка items
            if (template.getItems() != null && !template.getItems().isEmpty()) {
                List<TemplateItemRequest> items = template.getItems().stream()
                        .map(item -> {
                            TemplateItemRequest itemRequest = new TemplateItemRequest();
                            itemRequest.setName(item.getName());
                            itemRequest.setDescription(item.getDescription());
                            itemRequest.setDefaultQuantity(item.getDefaultQuantity() != null ? item.getDefaultQuantity() : 1);
                            return itemRequest;
                        })
                        .collect(Collectors.toList());
                templateRequest.setItems(items);
            } else {
                templateRequest.setItems(new ArrayList<>());
            }

            model.addAttribute("templateRequest", templateRequest);
            model.addAttribute("categories", TemplateCategory.values());
            model.addAttribute("isEdit", true); // Флаг для режима редактирования
            model.addAttribute("templateId", id); // ID для формы
            return "admin/template-form";
        } catch (Exception e) {
            log.error("Error loading edit form for template: {}", id, e);
            return "redirect:/admin/templates?error=not_found";
        }
    }

    @PostMapping("/templates/{id}/edit")
    public String updateTemplate(@PathVariable Long id,
                                 @ModelAttribute ChecklistTemplateRequest templateRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            ChecklistTemplate existingTemplate = templateService.getTemplateById(id);

            // Создаем обновленный шаблон
            ChecklistTemplate updatedTemplate = new ChecklistTemplate();
            updatedTemplate.setName(templateRequest.getName());
            updatedTemplate.setDescription(templateRequest.getDescription());
            updatedTemplate.setCategory(templateRequest.getCategory());

            // Обновляем элементы
            if (templateRequest.getItems() != null) {
                Set<TemplateItem> items = templateRequest.getItems().stream()
                        .map(itemRequest -> {
                            TemplateItem item = new TemplateItem();
                            item.setName(itemRequest.getName());
                            item.setDescription(itemRequest.getDescription());
                            item.setDefaultQuantity(itemRequest.getDefaultQuantity());
                            item.setTemplate(updatedTemplate);
                            return item;
                        })
                        .collect(Collectors.toSet());

                updatedTemplate.setItems(items);
            } else {
                updatedTemplate.setItems(new java.util.HashSet<>());
            }

            templateService.updateTemplate(id, updatedTemplate);
            redirectAttributes.addFlashAttribute("success", "Шаблон успешно обновлен");
        } catch (Exception e) {
            log.error("Error updating template: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении шаблона: " + e.getMessage());
        }
        return "redirect:/admin/templates";
    }

    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            templateService.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Шаблон успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении шаблона: " + e.getMessage());
        }
        return "redirect:/admin/templates";
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        try {
            Map<String, Object> stats = statisticsService.getAdminStatistics();

            model.addAttribute("totalUsers", stats.get("totalUsers"));
            model.addAttribute("totalEvents", stats.get("totalEvents"));
            model.addAttribute("totalCategories", stats.get("totalCategories"));
            model.addAttribute("upcomingEvents", stats.get("upcomingEvents"));
            model.addAttribute("averageParticipants", stats.get("averageParticipantsPerEvent"));
            model.addAttribute("totalComments", stats.get("totalComments"));
            model.addAttribute("totalChatMessages", stats.get("totalChatMessages"));
            model.addAttribute("totalTasks", stats.get("totalTasks"));
            model.addAttribute("totalChecklistItems", stats.get("totalChecklistItems"));
            model.addAttribute("completedTasksPercentage", stats.get("completedTasksPercentage"));
            model.addAttribute("completedChecklistPercentage", stats.get("completedChecklistItemsPercentage"));
            model.addAttribute("popularCategories", stats.get("mostPopularCategories"));

        } catch (Exception e) {
            log.error("Error loading admin dashboard", e);
            model.addAttribute("error", "Ошибка загрузки статистики");
        }

        return "admin/dashboard";
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewReport(Model model) {
        try {
            Map<String, Object> stats = statisticsService.getAdminStatistics();
            model.addAttribute("stats", stats);
            model.addAttribute("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            model.addAttribute("activeUsers", userService.getTotalUsersCount());
            model.addAttribute("totalEvents", eventService.getTotalEventsCount());

            log.info("Report page loaded successfully");

        } catch (Exception e) {
            log.error("Error loading report page", e);
            model.addAttribute("error", "Ошибка загрузки отчета: " + e.getMessage());
        }

        return "admin/report";
    }

    @PostMapping("/generate-report")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateReport() {
        // Просто перенаправляем на страницу отчета
        return "redirect:/admin/report";
    }

    @GetMapping("/generate-pdf-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> generatePdfReport() {
        try {
            Map<String, Object> stats = statisticsService.getDetailedStatistics();
            byte[] pdfBytes = pdfReportService.generateStatisticsReport(stats);

            String filename = "platform-report-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".pdf";

            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfBytes));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate-custom-report")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateCustomReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "all") String reportType,
            Model model) {

        try {
            Map<String, Object> customStats = statisticsService.getAdminStatistics();

            // Добавляем параметры отчета
            customStats.put("reportType", getReportTypeDisplayName(reportType));
            customStats.put("period", generatePeriodDescription(startDate, endDate));
            customStats.put("isCustomReport", true);

            model.addAttribute("stats", customStats);
            model.addAttribute("reportGenerated", true);
            model.addAttribute("isCustomReport", true);
            model.addAttribute("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

            log.info("Custom report generated - Type: {}, Period: {} to {}", reportType, startDate, endDate);

        } catch (Exception e) {
            log.error("Error generating custom report", e);
            model.addAttribute("error", "Ошибка при генерации пользовательского отчета: " + e.getMessage());
        }

        return "admin/dashboard";
    }

    private String getReportTypeDisplayName(String reportType) {
        switch (reportType) {
            case "users": return "Отчет по пользователям";
            case "events": return "Отчет по мероприятиям";
            case "activity": return "Отчет по активности";
            default: return "Полный отчет платформы";
        }
    }

    private String generatePeriodDescription(String startDate, String endDate) {
        if (startDate == null && endDate == null) {
            return "Все время";
        } else if (startDate != null && endDate != null) {
            return "Период: " + startDate + " - " + endDate;
        } else if (startDate != null) {
            return "С " + startDate;
        } else {
            return "По " + endDate;
        }
    }

    @GetMapping("/download-pdf-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> downloadPdfReport() {
        try {
            Map<String, Object> stats = statisticsService.getDetailedStatistics();
            byte[] pdfBytes = pdfReportService.generateStatisticsReport(stats);

            String filename = "platform-report-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";

            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfBytes));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/templates/{id}/view")
    public String viewTemplate(@PathVariable Long id, Model model) {
        try {
            ChecklistTemplate template = checklistTemplateService.getTemplateById(id);
            model.addAttribute("template", template);
            return "admin/template-view";
        } catch (Exception e) {
            return "redirect:/admin/templates?error=not_found";
        }
    }
}