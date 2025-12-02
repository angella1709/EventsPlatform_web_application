package com.example.angella.eventsplatform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Image extends BaseEntity {

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    private Long size;

    @Column(nullable = false)
    private String filePath;

    // Связи с другими сущностями (опционально, в зависимости от использования)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Для аватара пользователя

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event; // Для изображения события

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage; // Для изображений в чате
}