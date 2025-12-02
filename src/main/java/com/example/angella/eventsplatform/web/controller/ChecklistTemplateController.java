package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.mapper.ChecklistMapper;
import com.example.angella.eventsplatform.mapper.ChecklistTemplateMapper;
import com.example.angella.eventsplatform.service.ChecklistTemplateService;
import com.example.angella.eventsplatform.utils.AuthUtils;
import com.example.angella.eventsplatform.web.dto.ApplyTemplateRequest;
import com.example.angella.eventsplatform.web.dto.ChecklistItemDto;
import com.example.angella.eventsplatform.web.dto.ChecklistTemplateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/templates")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService templateService;
    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistMapper checklistMapper;

    @GetMapping
    public ResponseEntity<List<ChecklistTemplateDto>> getAllTemplates() {
        var templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templateMapper.toDtoList(templates));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ChecklistTemplateDto>> getTemplatesByCategory(
            @PathVariable String category) {
        var templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templateMapper.toDtoList(templates));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<List<ChecklistItemDto>> applyTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ApplyTemplateRequest request) {

        var items = templateService.applyTemplateToEvent(
                request.getTemplateId(),
                request.getEventId(),
                AuthUtils.getCurrentUserId(userDetails)
        );
        return ResponseEntity.ok(checklistMapper.toDtoList(items));
    }
}