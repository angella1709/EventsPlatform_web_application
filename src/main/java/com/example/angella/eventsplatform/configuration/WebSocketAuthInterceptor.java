package com.example.angella.eventsplatform.configuration;

import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.repository.EventRepository;
import com.example.angella.eventsplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private static final Pattern CHAT_PATTERN = Pattern.compile("/topic/chat/(\\d+)");
    private static final Pattern TASKS_PATTERN = Pattern.compile("/topic/tasks/(\\d+)");
    private static final Pattern CHECKLIST_PATTERN = Pattern.compile("/topic/checklist/(\\d+)");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination == null) return message;

            // Проверяем подписку на чат мероприятия
            if (destination.startsWith("/topic/chat/")) {
                return validateSubscription(message, accessor, destination, CHAT_PATTERN, "chat");
            }

            // Проверяем подписку на задачи
            if (destination.startsWith("/topic/tasks/")) {
                return validateSubscription(message, accessor, destination, TASKS_PATTERN, "tasks");
            }

            // Проверяем подписку на checklist
            if (destination.startsWith("/topic/checklist/")) {
                return validateSubscription(message, accessor, destination, CHECKLIST_PATTERN, "checklist");
            }
        }

        return message;
    }

    private Message<?> validateSubscription(Message<?> originalMessage,
                                            StompHeaderAccessor accessor,
                                            String destination,
                                            Pattern pattern,
                                            String type) {
        try {
            // 1. Извлекаем eventId из destination
            Matcher matcher = pattern.matcher(destination);
            if (!matcher.matches()) {
                log.warn("Invalid {} destination: {}", type, destination);
                return null; // Блокируем подписку
            }

            Long eventId = Long.parseLong(matcher.group(1));

            // 2. Получаем username из Principal
            if (accessor.getUser() == null) {
                log.warn("No user in WebSocket subscription");
                return null;
            }

            String username = accessor.getUser().getName();
            if (username == null) {
                log.warn("No username in WebSocket subscription");
                return null;
            }

            // 3. Находим пользователя
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // 4. участник ли?
            boolean isParticipant = eventRepository.existsByIdAndParticipantsId(eventId, user.getId());

            if (!isParticipant) {
                log.warn("User {} attempted to subscribe to {} {} without being participant",
                        username, type, eventId);
                return null; // Блокируем подписку
            }

            log.debug("Allowed subscription: user {} to {} {}", username, type, eventId);
            return originalMessage; // Возвращаем оригинальное сообщение

        } catch (NumberFormatException e) {
            log.warn("Invalid event ID in destination: {}", destination);
            return null;
        } catch (Exception e) {
            log.error("Error validating {} subscription", type, e);
            return null;
        }
    }
}