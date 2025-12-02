package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.ChatMessage;
import com.example.angella.eventsplatform.entity.Image;
import com.example.angella.eventsplatform.mapper.ImageMapper;
import com.example.angella.eventsplatform.service.ChatService;
import com.example.angella.eventsplatform.service.ImageService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import com.example.angella.eventsplatform.web.dto.ImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final ChatService chatService;
    private final ImageMapper imageMapper;

    // АВАТАР ПОЛЬЗОВАТЕЛЯ
    @PostMapping("/avatar")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<String> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        String imageUrl = imageService.uploadAvatar(
                file,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(imageUrl);
    }

    // ИЗОБРАЖЕНИЕ ДЛЯ СОБЫТИЯ
    @PostMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<String> uploadEventImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file) {

        String imageUrl = imageService.uploadEventImage(
                file,
                eventId,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(imageUrl);
    }

    // ИЗОБРАЖЕНИЕ ДЛЯ ЧАТА
    @PostMapping("/chat/{messageId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<String> uploadChatImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file) {

        ChatMessage message = chatService.addImageToMessage(
                messageId,
                file,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok("Image successfully added to message");
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ СООБЩЕНИЯ ЧАТА
    @GetMapping("/chat/{messageId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<ImageDto>> getChatMessageImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {

        List<Image> images = chatService.getMessageImages(messageId);
        List<ImageDto> imageDtos = images.stream()
                .map(imageMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(imageDtos);
    }

    // УДАЛЕНИЕ ИЗОБРАЖЕНИЯ ИЗ ЧАТА
    @DeleteMapping("/chat/{messageId}/{imageId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> removeChatImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @PathVariable Long imageId) {

        chatService.removeImageFromMessage(
                messageId,
                imageId,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.noContent().build();
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ СОБЫТИЯ
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ImageDto>> getEventImages(
            @PathVariable Long eventId) {

        List<Image> images = imageService.getEventImages(eventId);
        List<ImageDto> imageDtos = images.stream()
                .map(imageMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(imageDtos);
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ImageDto>> getUserImages(
            @PathVariable Long userId) {

        List<Image> images = imageService.getUserImages(userId);
        List<ImageDto> imageDtos = images.stream()
                .map(imageMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(imageDtos);
    }

    // УДАЛЕНИЕ ИЗОБРАЖЕНИЯ
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long imageId) {

        imageService.deleteImage(imageId, AuthUtils.getCurrentUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    // ПОЛУЧЕНИЕ ИНФОРМАЦИИ ОБ ИЗОБРАЖЕНИИ
    @GetMapping("/{imageId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ImageDto> getImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long imageId) {

        Image image = imageService.getImageById(
                imageId,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(imageMapper.toDto(image));
    }
}