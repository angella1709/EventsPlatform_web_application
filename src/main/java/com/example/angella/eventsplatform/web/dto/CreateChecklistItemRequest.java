package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateChecklistItemRequest {
    @NotBlank(message = "Название элемента не может быть пустым")
    private String name;

    private String description;

    @Positive(message = "Количество должно быть положительным числом")
    private Integer quantity = 1;

    private Long assignedUserId; // ID пользователя, которому назначен элемент (опционально)
}