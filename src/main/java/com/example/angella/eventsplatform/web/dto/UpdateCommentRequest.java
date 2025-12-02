package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCommentRequest {
    @NotBlank(message = "Comment text must not be blank!")
    private String text;
}