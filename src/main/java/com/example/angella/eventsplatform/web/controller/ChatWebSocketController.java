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

        String content = (String) payload.get("content");
        Long imageId = payload.get("imageId") != null ?
                Long.valueOf(payload.get("imageId").toString()) : null;

        log.info("=== WEBSOCKET START: eventId={}, user={}, hasImageId={} ===",
                eventId, principal.getName(), imageId != null);

        // Создаем сообщение
        ChatMessage savedMessage = chatService.createMessage(
                content, eventId, getUserId(principal)
        );

        log.info("1. Message created with ID: {}", savedMessage.getId());

        // Если есть imageId, привязываем изображение к сообщению
        if (imageId != null) {
            try {
                log.info("2. Attaching image {} to message {}", imageId, savedMessage.getId());

                // Находим изображение
                Image image = imageRepository.findById(imageId)
                        .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));

                // Привязываем к сообщению
                image.setChatMessage(savedMessage);
                imageRepository.save(image);

                log.info("3. Image attached successfully");

                // После прикрепления изображения отправляем команду обновления
                Map<String, Object> refreshCommand = new HashMap<>();
                refreshCommand.put("action", "FORCE_REFRESH");
                refreshCommand.put("messageId", savedMessage.getId());
                refreshCommand.put("eventId", eventId);
                refreshCommand.put("timestamp", System.currentTimeMillis());

                // Отправляем отдельную команду для обновления
                messagingTemplate.convertAndSend("/topic/chat/" + eventId + "/refresh", refreshCommand);
                log.info("4. Sent refresh command for chat");

            } catch (Exception e) {
                log.error("Failed to attach image: {}", e.getMessage());
            }
        }

        // Загружаем сообщение с изображениями
        ChatMessage messageWithImages = chatMessageRepository
                .findByIdWithImages(savedMessage.getId())
                .orElse(savedMessage);

        // Проверяем результат
        int imageCount = 0;
        if (messageWithImages.getImages() != null) {
            imageCount = messageWithImages.getImages().size();
            log.info("4. Found {} images in loaded message", imageCount);
        }

        // Возвращаем DTO
        ChatMessageDto dto = chatMessageMapper.toDto(messageWithImages);
        log.info("5. Returning DTO with {} images",
                dto.getImages() != null ? dto.getImages().size() : 0);

        log.info("=== WEBSOCKET END ===");

        return dto;
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