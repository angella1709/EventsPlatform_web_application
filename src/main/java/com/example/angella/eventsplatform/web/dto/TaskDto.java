package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private Long id;
    private String description;
    private boolean completed;
    private String creator;
    private String assignedUser;
    private Long assignedUserId;
    private Instant createdAt;

    public Long getAssignedUserId() {
        return assignedUserId;
    }
}