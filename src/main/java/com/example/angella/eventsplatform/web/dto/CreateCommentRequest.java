package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Text for comment must be set!")
    private String text;

}
