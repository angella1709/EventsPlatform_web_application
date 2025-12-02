package com.example.angella.eventsplatform.service.checker;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.service.EventAccessService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantCheckerService extends AbstractAccessCheckerService<ParticipantCheckerService.ParticipantAccessData> {

    private final EventAccessService eventAccessService;

    @Override
    protected boolean check(ParticipantAccessData accessData) {
        return eventAccessService.hasParticipant(accessData.eventId(), accessData.participantId());
    }

    @Override
    protected ParticipantAccessData getAccessData(HttpServletRequest request) {
        var eventId = getFromRequestParams(request, "eventId", Long::valueOf);
        return new ParticipantAccessData(eventId, AuthUtils.getAuthenticatedUser().getId());
    }

    @Override
    public AccessCheckType getType() {
        return AccessCheckType.PARTICIPANT;
    }

    protected record ParticipantAccessData(Long eventId, Long participantId) implements AccessData {
    }
}
