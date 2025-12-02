package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private String content;
    private String author;
    private Instant createdAt;
    private boolean edited;
    private List<ImageDto> images;
}