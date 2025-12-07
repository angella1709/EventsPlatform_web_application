package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.Image;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.mapper.ImageMapper;
import com.example.angella.eventsplatform.service.EventAccessService;
import com.example.angella.eventsplatform.service.ImageService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import com.example.angella.eventsplatform.web.dto.ImageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
public class ImageUploadController {

    private final ImageService imageService;
    private final EventAccessService eventAccessService;
    private final ImageMapper imageMapper;

    @PostMapping("/upload")
    public ResponseEntity<ImageDto> uploadImageForChat(
            @RequestParam Long eventId,
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        log.info("Загрузка изображения для события {}: {}", eventId, imageFile.getOriginalFilename());

        Long userId = AuthUtils.getCurrentUserId(userDetails);

        // Проверка прав
        if (!eventAccessService.hasParticipant(eventId, userId)) {
            throw new AccessDeniedException("Только участники события могут загружать изображения");
        }

        // Проверка размера файла
        if (imageFile.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: 10MB");
        }

        // Проверка типа файла
        if (!imageFile.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Файл должен быть изображением");
        }

        // Сохраняем изображение
        Image savedImage = imageService.saveTemporaryImage(imageFile, eventId, userId);

        log.info("Изображение сохранено: ID={}, filename={}",
                savedImage.getId(), savedImage.getFilename());

        return ResponseEntity.ok(imageMapper.toDto(savedImage));
    }
}