package com.example.angella.eventsplatform.web.dto;

import lombok.Data;

@Data
public class TemplateItemRequest {
    private String name;
    private String description;
    private Integer defaultQuantity = 1;
}