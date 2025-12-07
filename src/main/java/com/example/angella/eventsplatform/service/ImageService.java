package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.ImageRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageService {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Lazy  // Ленивая зависимость чтобы разорвать цикл
    private final UserService userService;


    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // АВАТАР ПОЛЬЗОВАТЕЛЯ
    public String uploadAvatar(MultipartFile file, Long userId) {
        try {
            User user = userService.findById(userId);

            // Удаляем старый аватар только если это не заглушка
            if (user.getAvatar() != null && !user.getAvatar().getFilename().equals("default-avatar.png")) {
                deleteImageFile(user.getAvatar());
                imageRepository.delete(user.getAvatar());
            }

            Image image = saveImage(file, user, null, null);
            user.setAvatar(image);
            userService.save(user);

            return "/images/" + image.getFilename();
        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            throw new RuntimeException("Failed to upload image");
        }
    }

    // ИЗОБРАЖЕНИЕ ДЛЯ СОБЫТИЯ
    public String uploadEventImage(MultipartFile file, Long eventId, Long userId) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            // Проверяем права доступа - только участники события могут загружать изображения
            if (!eventRepository.existsByIdAndParticipantsId(eventId, userId)) {
                throw new AccessDeniedException("Only event participants can upload images");
            }

            // Сохраняем изображение и связываем с событием
            Image image = saveImage(file, null, event, null);

            // Обновляем связь события с изображением
            if (event.getImages() == null) {
                event.setImages(new java.util.HashSet<>());
            }
            event.getImages().add(image);
            eventRepository.save(event);

            log.info("Image successfully uploaded and linked to event {}: {}", eventId, image.getFilename());

            return "/images/" + image.getFilename();
        } catch (IOException e) {
            log.error("Error uploading event image", e);
            throw new RuntimeException("Failed to upload image");
        }
    }

    // ИЗОБРАЖЕНИЕ ДЛЯ ЧАТА
    public Image saveImageForChat(ChatMessage chatMessage, MultipartFile file) throws IOException {
        Image image = saveImage(file, null, null, chatMessage);
        return image;
    }

    // ОСНОВНОЙ МЕТОД СОХРАНЕНИЯ ИЗОБРАЖЕНИЯ
    private Image saveImage(MultipartFile file, User user, Event event, ChatMessage chatMessage) throws IOException {
        log.info("Starting image upload: originalFilename={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 10MB limit");
        }

        // Проверка формата файла
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/gif");
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("Unsupported file format: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }

        String filename = UUID.randomUUID() + fileExtension;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalFilename(originalFilename);
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setFilePath(filePath.toString());

        // Устанавливаем связь в зависимости от типа entity
        if (user != null) {
            image.setUser(user);
        } else if (event != null) {
            image.setEvent(event);
        } else if (chatMessage != null) {
            image.setChatMessage(chatMessage);
        }

        Image savedImage = imageRepository.save(image);
        log.info("Image saved successfully: filename={}, id={}, path={}",
                filename, savedImage.getId(), filePath.toString());

        return savedImage;
    }

    // УДАЛЕНИЕ ФАЙЛА ИЗОБРАЖЕНИЯ
    private void deleteImageFile(Image image) {
        try {
            Path filePath = Paths.get(image.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Image file deleted: {}", image.getFilePath());
        } catch (IOException e) {
            log.warn("Failed to delete image file: {}", image.getFilePath(), e);
        }
    }

    // УДАЛЕНИЕ ИЗОБРАЖЕНИЯ
    public void deleteImage(Long imageId, Long userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));

        // Проверяем права доступа
        if (image.getUser() != null && !image.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot delete other user's image");
        }

        if (image.getChatMessage() != null && !image.getChatMessage().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot delete other user's chat images");
        }

        deleteImageFile(image);
        imageRepository.delete(image);
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ ПОЛЬЗОВАТЕЛЯ
    public List<Image> getUserImages(Long userId) {
        return imageRepository.findByUserId(userId);
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ СОБЫТИЯ
    public List<Image> getEventImages(Long eventId) {
        return imageRepository.findByEventId(eventId);
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЙ СООБЩЕНИЯ ЧАТА
    public List<Image> getChatMessageImages(Long chatMessageId) {
        return imageRepository.findByChatMessageId(chatMessageId);
    }

    // ПОЛУЧЕНИЕ ИЗОБРАЖЕНИЯ ПО ID С ПРОВЕРКОЙ ПРАВ
    public Image getImageById(Long imageId, Long userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));

        // Проверяем права доступа
        if (image.getUser() != null && !image.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot access other user's image");
        }

        return image;
    }

    public Image getDefaultAvatar() {
        // Ищем существующую заглушку в базе
        return imageRepository.findByFilename("default-avatar.png")
                .orElseGet(() -> createDefaultAvatar());
    }

    private Image createDefaultAvatar() {
        Image defaultAvatar = new Image();
        defaultAvatar.setFilename("default-avatar.png");
        defaultAvatar.setOriginalFilename("default-avatar.png");
        defaultAvatar.setContentType("image/png");
        defaultAvatar.setSize(0L);
        defaultAvatar.setFilePath("system/default-avatar.png");

        return imageRepository.save(defaultAvatar);
    }

    public Image saveTemporaryImage(MultipartFile imageFile, Long eventId, Long userId)
            throws IOException {

        // Находим событие и пользователя
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Генерируем уникальное имя файла
        String originalFilename = imageFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + fileExtension;

        // Создаем сущность Image
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalFilename(originalFilename);
        image.setContentType(imageFile.getContentType());
        image.setSize(imageFile.getSize());
        image.setFilePath(uploadDir + "/" + filename);
        image.setEvent(event);
        image.setUser(user);
        // chatMessage = null (привяжем позже)

        // Создаем директорию если не существует
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Сохраняем файл на диск
        Path filePath = uploadPath.resolve(filename);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Файл сохранен: {}", filePath);

        // Сохраняем в БД
        return imageRepository.save(image);
    }
}