package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateChecklistItemRequest {
    @NotBlank(message = "Название элемента не может быть пустым")
    private String name;

    private String description;

    @Positive(message = "Количество должно быть положительным числом")
    private Integer quantity;

    private Boolean completed;

    private Long assignedUserId;
}