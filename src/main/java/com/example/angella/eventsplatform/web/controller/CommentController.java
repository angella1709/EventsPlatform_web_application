package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.aop.AccessAnnotation;
import com.example.angella.eventsplatform.entity.Comment;
import com.example.angella.eventsplatform.mapper.CommentMapper;
import com.example.angella.eventsplatform.service.CommentService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import com.example.angella.eventsplatform.web.dto.CommentDto;
import com.example.angella.eventsplatform.web.dto.CreateCommentRequest;
import com.example.angella.eventsplatform.web.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private final CommentMapper commentMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<CommentDto> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateCommentRequest request,
            @RequestParam Long eventId) {

        try {
            var createdComment = commentService.save(
                    commentMapper.toEntity(request),
                    AuthUtils.getCurrentUserId(userDetails),
                    eventId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(commentMapper.toDto(createdComment));
        } catch (Exception e) {
            log.error("Error creating comment for event: {}", eventId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @AccessAnnotation(checkBy = AccessCheckType.COMMENT)
    public ResponseEntity<?> deleteComment(@PathVariable Long id, @RequestParam Long eventId) {
        commentService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @AccessAnnotation(checkBy = AccessCheckType.COMMENT)
    public ResponseEntity<CommentDto> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request) {

        Comment updatedComment = commentService.updateComment(
                id,
                request.getText(),
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(commentMapper.toDto(updatedComment));
    }
}
