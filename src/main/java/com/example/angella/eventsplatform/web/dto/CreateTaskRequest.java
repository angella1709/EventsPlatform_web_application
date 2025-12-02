package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "Описание задачи не может быть пустым!")
    @Size(max = 500, message = "Максимальная длина описания — 500 символов")
    private String description;

    private Long assignedUserId; // ДОБАВЛЕНО
}