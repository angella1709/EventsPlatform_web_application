package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.Comment;
import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.repository.CommentRepository;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    public List<Comment> findAllByEventId(Long eventId) {
        return findAllByEventId(eventId, null).getContent();
    }

    public Page<Comment> findAllByEventId(Long eventId, PageModel pageModel) {
        return commentRepository.findAllByEventId(
                eventId,
                pageModel == null ? Pageable.unpaged() : pageModel.toPageRequest()
        );
    }

    @Transactional
    public Comment save(Comment comment, Long userId, Long eventId) {
        var currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                MessageFormat.format("Event with id {0} not found!", eventId)
                        )
                );
        return userRepository.findById(userId)
                .map(user -> {
                    comment.setUser(user);
                    comment.setEvent(currentEvent);

                    return commentRepository.save(comment);
                })
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                MessageFormat.format("User with id {0} not found!", userId)
                        )
                );
    }

    @Transactional
    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }

    public boolean hasInEvent(Long commentId, Long eventId, Long authorId) {
        return commentRepository.existsByIdAndEventIdAndUserId(commentId, eventId, authorId);
    }

    public Long getTotalCommentsCount() {
        return commentRepository.count();
    }

    public Comment findById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Comment with id {0} not found!", id)
                ));
    }

    @Transactional
    public Comment updateComment(Long commentId, String newText, Long currentUserId) {
        Comment comment = findById(commentId);
        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only comment author can update the comment");
        }
        comment.setText(newText);
        return commentRepository.save(comment);
    }

    public boolean isCommentAuthor(Long commentId, Long userId) {
        return commentRepository.findById(commentId)
                .map(comment -> comment.getUser().getId().equals(userId))
                .orElse(false);
    }
}
