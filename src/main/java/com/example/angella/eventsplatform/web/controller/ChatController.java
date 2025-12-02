package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.aop.AccessAnnotation;
import com.example.angella.eventsplatform.entity.ChatMessage;
import com.example.angella.eventsplatform.entity.Image;
import com.example.angella.eventsplatform.mapper.ChatMessageMapper;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.repository.ChatMessageRepository;
import com.example.angella.eventsplatform.service.ChatService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import com.example.angella.eventsplatform.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.angella.eventsplatform.mapper.ImageMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageMapper chatMessageMapper;
    private final ImageMapper imageMapper;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<ChatMessageDto>> getMessages(
            @PathVariable Long eventId,
            PageModel pageModel) {
        var page = chatService.getMessages(eventId, pageModel);
        return ResponseEntity.ok(new PageResponse<>(
                page.getTotalElements(),
                page.getTotalPages(),
                page.map(chatMessageMapper::toDto).getContent()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @AccessAnnotation(checkBy = AccessCheckType.PARTICIPANT)
    public ResponseEntity<ChatMessageDto> createMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long eventId,
            @Valid @RequestBody CreateChatMessageRequest request) {

        var createdMessage = chatService.createMessage(
                request.getContent(),
                eventId,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatMessageMapper.toDto(createdMessage));
    }

    @PutMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ChatMessageDto> updateMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateChatMessageRequest request
    ) {
        var updatedMessage = chatService.updateMessage(
                messageId,
                request.getContent(),
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(chatMessageMapper.toDto(updatedMessage));
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        chatService.deleteMessage(messageId, AuthUtils.getCurrentUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/images")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ChatMessageDto> addImageToMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @RequestPart("image") MultipartFile imageFile) {

        ChatMessage message = chatService.addImageToMessage(
                messageId,
                imageFile,
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(chatMessageMapper.toDto(message));
    }

    @DeleteMapping("/{messageId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> removeImageFromMessage(
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

    @GetMapping("/{messageId}/images")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<List<ImageDto>> getMessageImages(
            @PathVariable Long messageId) {

        List<Image> images = chatService.getMessageImages(messageId);
        return ResponseEntity.ok(images.stream()
                .map(imageMapper::toDto)
                .collect(Collectors.toList()));
    }
}