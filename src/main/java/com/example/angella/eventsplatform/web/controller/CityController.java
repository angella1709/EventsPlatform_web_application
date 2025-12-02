package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.service.EventService;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/cities")
@RequiredArgsConstructor
public class CityController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<String>> getAllCities() {
        return ResponseEntity.ok(eventService.getAllCities());
    }

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchCities(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(eventService.searchCities(query));
    }
}