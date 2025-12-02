package com.example.angella.eventsplatform.web.dto;

import lombok.Data;

@Data
public class ApplyTemplateRequest {
    private Long templateId;
    private Long eventId;
}