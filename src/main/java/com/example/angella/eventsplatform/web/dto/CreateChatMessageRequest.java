package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatMessageRequest {
    @NotBlank(message = "Текст сообщения не может быть пустым!")
    @Size(max = 1000, message = "Максимальная длина сообщения — 1000 символов")
    private String content;
}