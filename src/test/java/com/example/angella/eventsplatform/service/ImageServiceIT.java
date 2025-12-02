package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.*;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ImageServiceIT extends ServiceIntegrationTest {

    @Autowired private ImageService imageService;
    @Autowired private UserService userService;
    @Autowired private EventService eventService;
    @Autowired private ChatService chatService;
    @Autowired private LocationRepository locationRepository;

    private User user1;
    private User user2;
    private Event testEvent;
    private ChatMessage testMessage;
    private MultipartFile testImageJpg;
    private MultipartFile testImagePng;
    private MultipartFile testImageGif;
    private MultipartFile largeFile;
    private MultipartFile invalidFile;

    @BeforeEach
    void setUp() throws IOException {
        // Создаем пользователей
        user1 = createUser("user1", "user1@test.com");
        user2 = createUser("user2", "user2@test.com");

        // Создаем тестовое мероприятие
        testEvent = createTestEvent(user1);
        eventService.addParticipant(testEvent.getId(), user2.getId());

        // Создаем тестовое сообщение в чате
        testMessage = chatService.createMessage("Test message", testEvent.getId(), user1.getId());

        // Создаем тестовые файлы
        testImageJpg = createMockImageFile("test.jpg", "image/jpeg", 1024); // 1KB
        testImagePng = createMockImageFile("test.png", "image/png", 2048); // 2KB
        testImageGif = createMockImageFile("test.gif", "image/gif", 3072); // 3KB
        largeFile = createMockImageFile("large.jpg", "image/jpeg", 11 * 1024 * 1024); // 11MB
        invalidFile = createMockTextFile("test.txt", "text/plain", 1024);
    }

    @Test
    void uploadAvatar_ShouldSaveAndLinkAvatarToUser() throws IOException {
        // Act
        String imageUrl = imageService.uploadAvatar(testImageJpg, user1.getId());

        // Assert
        assertNotNull(imageUrl);
        assertTrue(imageUrl.startsWith("/images/"));

        // Проверяем что аватар привязан к пользователю
        User updatedUser = userService.findById(user1.getId());
        assertNotNull(updatedUser.getAvatar());
        assertEquals("test.jpg", updatedUser.getAvatar().getOriginalFilename());
    }

    @Test
    void uploadAvatar_ShouldReplaceOldAvatar() throws IOException {
        // Arrange - загружаем первый аватар
        imageService.uploadAvatar(testImageJpg, user1.getId());
        User userAfterFirstUpload = userService.findById(user1.getId());
        Long firstAvatarId = userAfterFirstUpload.getAvatar().getId();

        // Act - загружаем второй аватар
        String newImageUrl = imageService.uploadAvatar(testImagePng, user1.getId());

        // Assert
        User userAfterSecondUpload = userService.findById(user1.getId());
        assertNotNull(newImageUrl);
        assertNotEquals(firstAvatarId, userAfterSecondUpload.getAvatar().getId());
        assertEquals("test.png", userAfterSecondUpload.getAvatar().getOriginalFilename());
    }

    @Test
    void uploadEventImage_ShouldSaveAndLinkToEvent() throws IOException {
        // Act
        String imageUrl = imageService.uploadEventImage(testImageJpg, testEvent.getId(), user1.getId());

        // Assert
        assertNotNull(imageUrl);
        assertTrue(imageUrl.startsWith("/images/"));

        // Проверяем что изображение привязано к мероприятию
        List<Image> eventImages = imageService.getEventImages(testEvent.getId());
        assertFalse(eventImages.isEmpty());
        assertEquals("test.jpg", eventImages.get(0).getOriginalFilename());
    }

    @Test
    void uploadEventImage_ByNonParticipant_ShouldThrowAccessDenied() {
        // Arrange
        User nonParticipant = createUser("nonparticipant", "non@test.com");

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                imageService.uploadEventImage(testImageJpg, testEvent.getId(), nonParticipant.getId())
        );
    }

    @Test
    void saveImageForChat_ShouldSaveAndLinkToMessage() throws IOException {
        // Act
        Image image = imageService.saveImageForChat(testMessage, testImageJpg);

        // Assert
        assertNotNull(image.getId());
        assertEquals("test.jpg", image.getOriginalFilename());
        assertEquals(testMessage.getId(), image.getChatMessage().getId());
    }

    @Test
    void getEventImages_ShouldReturnAllEventImages() throws IOException {
        // Arrange
        imageService.uploadEventImage(testImageJpg, testEvent.getId(), user1.getId());
        imageService.uploadEventImage(testImagePng, testEvent.getId(), user2.getId());

        // Act
        List<Image> images = imageService.getEventImages(testEvent.getId());

        // Assert
        assertEquals(2, images.size());
        assertTrue(images.stream().anyMatch(img -> "test.jpg".equals(img.getOriginalFilename())));
        assertTrue(images.stream().anyMatch(img -> "test.png".equals(img.getOriginalFilename())));
    }

    @Test
    void getUserImages_ShouldReturnUserImages() throws IOException {
        // Arrange
        imageService.uploadAvatar(testImageJpg, user1.getId());
        imageService.uploadEventImage(testImagePng, testEvent.getId(), user1.getId());

        // Act
        List<Image> userImages = imageService.getUserImages(user1.getId());

        // Assert
        assertFalse(userImages.isEmpty());
        assertEquals(user1.getId(), userImages.get(0).getUser().getId());
    }

    @Test
    void getChatMessageImages_ShouldReturnMessageImages() throws IOException {
        // Arrange
        imageService.saveImageForChat(testMessage, testImageJpg);

        // Act
        List<Image> messageImages = imageService.getChatMessageImages(testMessage.getId());

        // Assert
        assertFalse(messageImages.isEmpty());
        assertEquals(testMessage.getId(), messageImages.get(0).getChatMessage().getId());
    }

    @Test
    void deleteImage_ByNonOwner_ShouldThrowAccessDenied() throws IOException {
        // Arrange
        imageService.uploadAvatar(testImageJpg, user1.getId());
        User user = userService.findById(user1.getId());
        Long imageId = user.getAvatar().getId();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                imageService.deleteImage(imageId, user2.getId())
        );
    }

    @Test
    void getImageById_ShouldReturnImageWithAccessCheck() throws IOException {
        // Arrange
        imageService.uploadAvatar(testImageJpg, user1.getId());
        User user = userService.findById(user1.getId());
        Long imageId = user.getAvatar().getId();

        // Act
        Image image = imageService.getImageById(imageId, user1.getId());

        // Assert
        assertNotNull(image);
        assertEquals(imageId, image.getId());
        assertEquals("test.jpg", image.getOriginalFilename());
    }

    @Test
    void getImageById_ForOtherUserImage_ShouldThrowAccessDenied() throws IOException {
        // Arrange
        imageService.uploadAvatar(testImageJpg, user1.getId());
        User user = userService.findById(user1.getId());
        Long imageId = user.getAvatar().getId();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                imageService.getImageById(imageId, user2.getId())
        );
    }

    @Test
    void uploadImage_WithDifferentFormats_ShouldSupportAll() throws IOException {
        // Act & Assert - JPG
        String jpgUrl = imageService.uploadAvatar(testImageJpg, user1.getId());
        assertNotNull(jpgUrl);

        // Act & Assert - PNG
        String pngUrl = imageService.uploadAvatar(testImagePng, user2.getId());
        assertNotNull(pngUrl);

        // Act & Assert - GIF
        String gifUrl = imageService.uploadEventImage(testImageGif, testEvent.getId(), user1.getId());
        assertNotNull(gifUrl);
    }

    @Test
    void uploadImage_WithLargeFile_ShouldThrowException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                imageService.uploadAvatar(largeFile, user1.getId())
        );
    }

    @Test
    void uploadImage_WithInvalidFormat_ShouldThrowException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                imageService.uploadAvatar(invalidFile, user1.getId())
        );
    }

    @Test
    void getDefaultAvatar_ShouldReturnDefaultImage() {
        // Act
        Image defaultAvatar = imageService.getDefaultAvatar();

        // Assert
        assertNotNull(defaultAvatar);
        assertEquals("default-avatar.png", defaultAvatar.getFilename());
    }

    @Test
    void multipleImageUploads_ShouldNotConflict() throws IOException {
        // Arrange
        int uploadCount = 5;

        // Act - множественная загрузка
        for (int i = 0; i < uploadCount; i++) {
            MultipartFile file = createMockImageFile("test" + i + ".jpg", "image/jpeg", 1024);
            imageService.uploadEventImage(file, testEvent.getId(), user1.getId());
        }

        // Assert
        List<Image> eventImages = imageService.getEventImages(testEvent.getId());
        assertEquals(uploadCount, eventImages.size());

        // Проверяем что все файлы имеют уникальные имена
        long uniqueFilenames = eventImages.stream()
                .map(Image::getFilename)
                .distinct()
                .count();
        assertEquals(uploadCount, uniqueFilenames);
    }

    // Вспомогательные методы
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        return userService.registerUser(user);
    }

    private Event createTestEvent(User creator) {
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("Test Event Description");
        event.setStartTime(Instant.now().plusSeconds(3600));
        event.setEndTime(Instant.now().plusSeconds(7200));

        Location location = new Location();
        location.setCity("Test City");
        location.setStreet("Test Street");
        location = locationRepository.save(location);
        event.setLocation(location);

        event.setCreator(creator);
        return eventService.create(event, creator.getId());
    }

    private MockMultipartFile createMockImageFile(String filename, String contentType, int size) {
        byte[] content = new byte[size];
        // Заполняем массив тестовыми данными
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i % 256);
        }
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private MockMultipartFile createMockTextFile(String filename, String contentType, int size) {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) ('A' + (i % 26));
        }
        return new MockMultipartFile("file", filename, contentType, content);
    }
}