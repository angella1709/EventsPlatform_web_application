package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAccessService {

    private final EventRepository eventRepository;

    public boolean isEventCreator(Long eventId, Long userId) {
        return eventRepository.existsByIdAndCreatorId(eventId, userId);
    }

    public boolean hasParticipant(Long eventId, Long participantId) {
        return eventRepository.existsByIdAndParticipantsId(eventId, participantId);
    }

    public boolean canRemoveParticipant(Long eventId, Long currentUserId, Long participantId) {
        return isEventCreator(eventId, currentUserId) || currentUserId.equals(participantId);
    }
}