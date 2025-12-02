package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResponse {
    private boolean success;
    private String message;
    private String imageUrl;
    private Long imageId;
}