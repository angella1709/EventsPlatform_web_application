package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.mapper.ChatMessageMapper;
import com.example.angella.eventsplatform.mapper.ChecklistMapper;
import com.example.angella.eventsplatform.mapper.TaskMapper;
import com.example.angella.eventsplatform.repository.ChatMessageRepository;
import com.example.angella.eventsplatform.repository.ImageRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import com.example.angella.eventsplatform.service.ChatService;
import com.example.angella.eventsplatform.service.TaskService;
import com.example.angella.eventsplatform.service.ChecklistService;
import com.example.angella.eventsplatform.web.dto.ChatMessageDto;
import com.example.angella.eventsplatform.web.dto.ChecklistItemDto;
import com.example.angella.eventsplatform.web.dto.TaskDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final TaskService taskService;
    private final ChecklistService checklistService;
    private final ChatMessageMapper chatMessageMapper;
    private final TaskMapper taskMapper;
    private final ChecklistMapper checklistMapper;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ImageRepository imageRepository;

    private Long getUserId(Principal principal) {
        if (principal == null) return null;

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getId();
    }

    @MessageMapping("/chat/{eventId}/send")
    @SendTo("/topic/chat/{eventId}")
    public ChatMessageDto sendMessage(@DestinationVariable Long eventId,
                                      @Payload Map<String, Object> payload,
                                      Principal principal) {

        log.info("WebSocket сообщение получено. Event: {}, Payload: {}", eventId, payload);

        try {
            // Извлекаем данные
            String content = (String) payload.get("content");
            Long userId = getUserId(principal);

            // Проверяем контент
            if (content == null || content.trim().isEmpty()) {
                content = "Изображение";
            }

            log.info("Создание сообщения: user={}, event={}, content={}",
                    userId, eventId, content.substring(0, Math.min(content.length(), 50)));

            // Создаем сообщение
            ChatMessage savedMessage = chatService.createMessage(content, eventId, userId);
            log.info("Сообщение создано, ID: {}", savedMessage.getId());

            // Обрабатываем изображения если есть
            Object imageIdsObj = payload.get("imageIds");
            if (imageIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> imageIdsInt = (List<Integer>) imageIdsObj;

                if (!imageIdsInt.isEmpty()) {
                    log.info("Прикрепляем {} изображений к сообщению {}",
                            imageIdsInt.size(), savedMessage.getId());

                    List<Long> imageIds = imageIdsInt.stream()
                            .map(Long::valueOf)
                            .collect(Collectors.toList());

                    attachImagesToMessage(savedMessage.getId(), imageIds, userId);
                }
            }

            // Загружаем сообщение с изображениями
            ChatMessage messageWithImages = chatMessageRepository
                    .findByIdWithImages(savedMessage.getId())
                    .orElse(savedMessage);

            log.info("Сообщение готово к отправке. Изображений: {}",
                    messageWithImages.getImages() != null ?
                            messageWithImages.getImages().size() : 0);

            // Преобразуем в DTO и отправляем
            return chatMessageMapper.toDto(messageWithImages);

        } catch (Exception e) {
            log.error("Ошибка обработки WebSocket сообщения:", e);
            // Возвращаем пустой DTO или сообщение об ошибке
            ChatMessageDto errorDto = new ChatMessageDto();
            errorDto.setContent("Ошибка отправки сообщения");
            errorDto.setAuthor("Система");
            errorDto.setCreatedAt(Instant.now());
            return errorDto;
        }
    }

    private void attachImagesToMessage(Long messageId, List<Long> imageIds, Long userId) {
        for (Long imageId : imageIds) {
            try {
                log.debug("Прикрепление изображения {} к сообщению {}", imageId, messageId);

                Image image = imageRepository.findById(imageId)
                        .orElseThrow(() -> {
                            log.warn("Изображение {} не найдено", imageId);
                            return new EntityNotFoundException("Изображение не найдено: " + imageId);
                        });

                // Проверяем права (только владелец изображения может прикрепить)
                if (!image.getUser().getId().equals(userId)) {
                    log.warn("Пользователь {} пытается прикрепить чужое изображение {}",
                            userId, imageId);
                    continue;
                }

                // Прикрепляем к сообщению
                image.setChatMessage(chatMessageRepository.getReferenceById(messageId));
                imageRepository.save(image);

                log.info("Изображение {} прикреплено к сообщению {}", imageId, messageId);

            } catch (Exception e) {
                log.error("Ошибка прикрепления изображения {}: {}", imageId, e.getMessage());
            }
        }
    }

    @MessageMapping("/tasks/{eventId}/update")
    @SendTo("/topic/tasks/{eventId}")
    public List<TaskDto> updateTasks(@DestinationVariable Long eventId) {
        List<Task> tasks = taskService.getTasksForEvent(eventId);
        return tasks.stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @MessageMapping("/checklist/{eventId}/update")
    @SendTo("/topic/checklist/{eventId}")
    public List<ChecklistItemDto> updateChecklist(@DestinationVariable Long eventId) {
        List<ChecklistItem> checklist = checklistService.getChecklistForEvent(eventId);
        return checklist.stream()
                .map(checklistMapper::toDto)
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat/{eventId}/images")
    public void handleImageUpload(@DestinationVariable Long eventId,
                                  @Payload ImageUploadMessage message,
                                  Principal principal) {
        // Отправляем уведомление всем участникам чата
        messagingTemplate.convertAndSend(
                "/topic/chat/" + eventId + "/images",
                Map.of(
                        "messageId", message.getMessageId(),
                        "imageUrl", message.getImageUrl(),
                        "author", principal.getName(),
                        "timestamp", Instant.now()
                )
        );
    }

    // DTO для загрузки изображений
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUploadMessage {
        private Long messageId;
        private String imageUrl;
        private String fileName;
    }
}