package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private List<TemplateItemDto> items;
}