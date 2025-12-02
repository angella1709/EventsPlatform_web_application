package com.example.angella.eventsplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessage extends BaseEntity {

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatMessage", cascade = CascadeType.ALL)
    private Set<Image> images;

    private boolean edited;
}