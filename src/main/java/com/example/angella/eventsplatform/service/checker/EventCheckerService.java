package com.example.angella.eventsplatform.service.checker;

import com.example.angella.eventsplatform.aop.AccessCheckType;
import com.example.angella.eventsplatform.service.EventAccessService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventCheckerService extends AbstractAccessCheckerService<EventCheckerService.EventAccessData> {

    private final EventAccessService eventAccessService;

    @Override
    protected boolean check(EventAccessData accessData) {
        return eventAccessService.isEventCreator(
                accessData.eventId,
                accessData.currentUserId
        );
    }

    @Override
    protected EventAccessData getAccessData(HttpServletRequest request) {
        var eventId = getFromPathVariable(
                request,
                "id",
                Long::valueOf
        );

        return new EventAccessData(eventId, AuthUtils.getAuthenticatedUser().getId());
    }

    @Override
    public AccessCheckType getType() {
        return AccessCheckType.EVENT;
    }

    protected record EventAccessData(Long eventId, Long currentUserId) implements AccessData {
    }
}