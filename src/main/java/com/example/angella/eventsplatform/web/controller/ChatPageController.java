package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.ChatMessage;
import com.example.angella.eventsplatform.entity.Event;
import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.service.EventService;
import com.example.angella.eventsplatform.service.TaskService;
import com.example.angella.eventsplatform.service.ChecklistService;
import com.example.angella.eventsplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import com.example.angella.eventsplatform.mapper.ChatMessageMapper;
import com.example.angella.eventsplatform.model.PageModel;
import com.example.angella.eventsplatform.service.ChatService;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatPageController {

    private final EventService eventService;
    private final UserService userService;
    private final TaskService taskService;
    private final ChecklistService checklistService;
    private final ChatService chatService;
    private final ChatMessageMapper chatMessageMapper;

    @GetMapping
    public String chatsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            User user = userService.findByUsername(userDetails.getUsername());

            // Получаем ВСЕ мероприятия пользователя (будущие и прошедшие)
            List<Event> allUserEvents = eventService.findAllUserEvents(user.getId());

            model.addAttribute("events", allUserEvents);
            model.addAttribute("currentUser", user);
            return "chats/list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки чатов: " + e.getMessage());
            return "chats/list";
        }
    }

    @GetMapping("/{eventId}")
    public String chatRoom(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long eventId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "50") int size,
                           Model model) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            User user = userService.findByUsername(userDetails.getUsername());
            Event event = eventService.getEventForDetailView(eventId);

            // Проверяем, что пользователь является участником события
            if (!event.getParticipants().stream()
                    .anyMatch(participant -> participant.getId().equals(user.getId()))) {
                return "redirect:/chats?error=access_denied";
            }

            // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Загружаем сообщения чата
            PageModel pageModel = new PageModel(page, size);
            List<ChatMessage> chatMessages = chatService.getMessages(eventId, pageModel).getContent();

            // Преобразуем в DTO для отображения
            var chatMessageDtos = chatMessages.stream()
                    .map(chatMessageMapper::toDto)
                    .collect(Collectors.toList());

            model.addAttribute("event", event);
            model.addAttribute("currentUser", user);
            model.addAttribute("tasks", taskService.getTasksForEvent(eventId));
            model.addAttribute("checklist", checklistService.getChecklistForEvent(eventId));
            model.addAttribute("participants", event.getParticipants());
            model.addAttribute("chatMessages", chatMessageDtos); // Добавляем сообщения
            model.addAttribute("page", page);
            model.addAttribute("size", size);

            return "chats/room";
        } catch (Exception e) {
            return "redirect:/chats?error=not_found";
        }
    }
}