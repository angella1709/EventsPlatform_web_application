package com.example.angella.eventsplatform.web.dto;

import com.example.angella.eventsplatform.entity.TemplateCategory;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChecklistTemplateRequest {
    private String name;
    private String description;
    private TemplateCategory category;
    private List<TemplateItemRequest> items = new ArrayList<>();
}