package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.ChatMessage;
import com.example.angella.eventsplatform.entity.Event;
import com.example.angella.eventsplatform.entity.Image;
import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.repository.ChatMessageRepository;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(Long eventId, PageModel pageModel) {
        // Проверяем существование события
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }

        return chatMessageRepository.findAllByEventId(
                eventId,
                pageModel == null ? Pageable.unpaged() : pageModel.toPageRequest()
        );
    }

    public ChatMessage createMessage(String content, Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Event with id {0} not found!", eventId)
                ));

        if (!eventRepository.existsByIdAndParticipantsId(eventId, userId)) {
            throw new AccessDeniedException("Only event participants can post messages");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("User with id {0} not found!", userId)
                ));

        ChatMessage message = new ChatMessage();
        message.setContent(content);
        message.setEvent(event);
        message.setAuthor(user);
        message.setImages(new java.util.HashSet<>());
        message.setEdited(false);

        return chatMessageRepository.save(message);
    }

    public ChatMessage updateMessage(Long messageId, String newContent, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Only author can edit the message");
        }

        message.setContent(newContent);
        message.setEdited(true);
        return chatMessageRepository.save(message);
    }

    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Message with id {0} not found!", messageId)
                ));

        if (!message.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Only message author can delete the message");
        }

        if (message.getImages() != null && !message.getImages().isEmpty()) {
            for (Image image : message.getImages()) {
                imageService.deleteImage(image.getId(), userId);
            }
        }

        chatMessageRepository.deleteById(messageId);
    }

    public boolean isMessageAuthor(Long messageId, Long userId) {
        return chatMessageRepository.existsByIdAndAuthorId(messageId, userId);
    }

    @Transactional
    public ChatMessage addImageToMessage(Long messageId, MultipartFile imageFile, Long userId) {
        log.info("Adding image to message {} by user {}", messageId, userId);

        // Явно загружаем сообщение с изображениями
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.error("Message {} not found", messageId);
                    return new EntityNotFoundException("Message not found");
                });

        if (!message.getAuthor().getId().equals(userId)) {
            log.warn("User {} tried to add image to message {} owned by {}",
                    userId, messageId, message.getAuthor().getId());
            throw new AccessDeniedException("Only message author can add images");
        }

        try {
            // Сохраняем изображение
            Image image = imageService.saveImageForChat(message, imageFile);
            log.info("Image saved: {} (id={})", image.getFilename(), image.getId());

            // Инициализируем коллекцию если null
            if (message.getImages() == null) {
                message.setImages(new HashSet<>());
            }

            // Добавляем изображение в коллекцию
            message.getImages().add(image);
            //message.setEdited(true);

            // Сохраняем сообщение
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("Message {} saved with {} images", savedMessage.getId(),
                    savedMessage.getImages() != null ? savedMessage.getImages().size() : 0);

            // Явно загружаем изображения
            if (savedMessage.getImages() != null) {
                savedMessage.getImages().size();
            }

            // Отправляем уведомление
            sendImageUpdateNotification(savedMessage, image);

            return savedMessage;
        } catch (IOException e) {
            log.error("Failed to upload image for message {}", messageId, e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    public List<Image> getMessageImages(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (message.getImages() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(message.getImages());
    }

    public void removeImageFromMessage(Long messageId, Long imageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Only message author can remove images");
        }

        Image imageToRemove = message.getImages().stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Image not found in this message"));

        message.getImages().remove(imageToRemove);
        message.setEdited(true);

        imageService.deleteImage(imageId, userId);

        chatMessageRepository.save(message);
    }

    public Long getTotalMessagesCount() {
        return chatMessageRepository.count();
    }

    private void sendImageUpdateNotification(ChatMessage message, Image image) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "IMAGE_ADDED");
            payload.put("messageId", message.getId());
            payload.put("eventId", message.getEvent().getId());
            payload.put("image", Map.of(
                    "id", image.getId(),
                    "url", "/images/" + image.getFilename(),
                    "originalFilename", image.getOriginalFilename()
            ));
            payload.put("author", message.getAuthor().getUsername());
            payload.put("timestamp", Instant.now());

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getEvent().getId() + "/images",
                    payload
            );

            log.info("WebSocket notification sent for image {} in message {}",
                    image.getId(), message.getId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for image", e);
        }
    }
}