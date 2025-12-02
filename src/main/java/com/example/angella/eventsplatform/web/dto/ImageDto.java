package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {
    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long size;
    private String url;
    private Instant createdAt;
}