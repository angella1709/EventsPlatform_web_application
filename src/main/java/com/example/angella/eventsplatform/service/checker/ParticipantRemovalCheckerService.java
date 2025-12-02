package com.example.angella.eventsplatform.service.checker;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.service.EventAccessService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantRemovalCheckerService
        extends AbstractAccessCheckerService<ParticipantRemovalCheckerService.ParticipantRemovalAccessData> {

    private final EventAccessService eventAccessService;

    @Override
    protected boolean check(ParticipantRemovalAccessData accessData) {
        return eventAccessService.canRemoveParticipant(
                accessData.eventId(),
                accessData.currentUserId(),
                accessData.participantId()
        );
    }

    @Override
    protected ParticipantRemovalAccessData getAccessData(HttpServletRequest request) {
        Long eventId = getFromPathVariable(request, "eventId", Long::valueOf);
        Long participantId = getFromPathVariable(request, "participantId", Long::valueOf);

        return new ParticipantRemovalAccessData(
                eventId,
                AuthUtils.getAuthenticatedUser().getId(),
                participantId
        );
    }

    @Override
    public AccessCheckType getType() {
        return AccessCheckType.PARTICIPANT_REMOVAL;
    }

    protected record ParticipantRemovalAccessData(
            Long eventId,
            Long currentUserId,
            Long participantId
    ) implements AccessData {}
}