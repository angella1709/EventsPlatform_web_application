package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String firstName;

    private String lastName;
}