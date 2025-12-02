package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDto {
    private Long id;
    private String name;
    private String description;
    private Integer quantity;
    private boolean completed;
    private Instant createdAt;
    private String createdBy;
    private String assignedUser;
    private Boolean fromTemplate;
}