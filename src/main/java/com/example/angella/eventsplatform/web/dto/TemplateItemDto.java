package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateItemDto {
    private Long id;
    private String name;
    private String description;
    private Integer defaultQuantity;
}