package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.ChecklistItem;
import com.example.angella.eventsplatform.entity.ChecklistTemplate;
import com.example.angella.eventsplatform.entity.Event;
import com.example.angella.eventsplatform.entity.TemplateItem;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.ChecklistTemplateRepository;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.TemplateItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChecklistTemplateService {

    private final ChecklistTemplateRepository templateRepository;
    private final TemplateItemRepository templateItemRepository;
    private final EventRepository eventRepository;
    private final ChecklistService checklistService;

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public List<ChecklistTemplate> getAllTemplates() {
        try {
            List<ChecklistTemplate> templates = templateRepository.findAll();
            // Безопасная инициализация ленивых коллекций
            templates.forEach(template -> {
                try {
                    if (template.getItems() != null) {
                        template.getItems().size(); // Инициализация коллекции
                    }
                } catch (Exception e) {
                    log.warn("Failed to initialize items for template {}: {}", template.getId(), e.getMessage());
                }
            });
            return templates;
        } catch (Exception e) {
            log.error("Error loading all templates", e);
            return List.of();
        }
    }

    public List<ChecklistTemplate> getTemplatesByCategory(String category) {
        try {
            com.example.angella.eventsplatform.entity.TemplateCategory templateCategory =
                    com.example.angella.eventsplatform.entity.TemplateCategory.valueOf(category.toUpperCase());
            List<ChecklistTemplate> templates = templateRepository.findByCategory(templateCategory);
            // Безопасная инициализация
            templates.forEach(template -> {
                try {
                    if (template.getItems() != null) {
                        template.getItems().size();
                    }
                } catch (Exception e) {
                    log.warn("Failed to initialize items for template {}: {}", template.getId(), e.getMessage());
                }
            });
            return templates;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid category: {}", category);
            return List.of();
        }
    }

    public List<ChecklistItem> applyTemplateToEvent(Long templateId, Long eventId, Long userId) {
        ChecklistTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Безопасная загрузка элементов шаблона
        List<TemplateItem> templateItems;
        try {
            templateItems = templateItemRepository.findByTemplateId(templateId);
        } catch (Exception e) {
            log.error("Error loading template items for template {}: {}", templateId, e.getMessage());
            templateItems = List.of();
        }

        return templateItems.stream()
                .map(templateItem -> checklistService.createItem(
                        templateItem.getName(),
                        templateItem.getDescription(),
                        templateItem.getDefaultQuantity(),
                        eventId,
                        userId,
                        null
                ))
                .toList();
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ChecklistTemplate getTemplateById(Long id) {
        ChecklistTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Template with id {0} not found!", id)
                ));

        // Безопасная инициализация ленивой коллекции
        try {
            if (template.getItems() != null) {
                template.getItems().size();
            }
        } catch (Exception e) {
            log.warn("Failed to initialize items for template {}: {}", id, e.getMessage());
        }

        return template;
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ChecklistTemplate updateTemplate(Long id, ChecklistTemplate updatedTemplate) {
        ChecklistTemplate template = getTemplateById(id);
        template.setName(updatedTemplate.getName());
        template.setDescription(updatedTemplate.getDescription());
        template.setCategory(updatedTemplate.getCategory());

        // Очищаем старые элементы
        if (template.getItems() != null) {
            template.getItems().clear();
        } else {
            template.setItems(new HashSet<>());
        }

        // Добавляем новые элементы
        if (updatedTemplate.getItems() != null) {
            for (TemplateItem item : updatedTemplate.getItems()) {
                item.setTemplate(template);
                template.getItems().add(item);
            }
        }

        return templateRepository.save(template);
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ChecklistTemplate createTemplate(ChecklistTemplate template) {
        // Убедимся, что коллекция items инициализирована
        if (template.getItems() == null) {
            template.setItems(new HashSet<>());
        }
        return templateRepository.save(template);
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public void deleteTemplate(Long templateId) {
        // Сначала удаляем элементы шаблона
        templateItemRepository.deleteByTemplateId(templateId);
        // Затем удаляем сам шаблон
        templateRepository.deleteById(templateId);
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public List<ChecklistTemplate> getAllTemplatesWithItems() {
        try {
            List<ChecklistTemplate> templates = templateRepository.findAll();

            // Безопасная инициализация ленивых коллекций
            templates.forEach(template -> {
                try {
                    if (template.getItems() != null) {
                        template.getItems().size();
                    }
                } catch (Exception e) {
                    log.warn("Failed to initialize items for template {}: {}", template.getId(), e.getMessage());
                }
            });

            return templates;
        } catch (Exception e) {
            log.error("Error loading templates with items", e);
            return List.of();
        }
    }
}