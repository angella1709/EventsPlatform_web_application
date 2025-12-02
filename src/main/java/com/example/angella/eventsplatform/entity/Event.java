package com.example.angella.eventsplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "events_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.ALL)
    private Set<Comment> comments = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.ALL)
    private Set<ChatMessage> chatMessages = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.ALL)
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.ALL)
    private Set<Image> images;

    public boolean addParticipant(User participant) {
        return participants.add(participant) && participant.addEvent(this);
    }

    public boolean removeParticipant(User participant) {
        boolean removed = participants.removeIf(p -> p.getId().equals(participant.getId()));
        if (removed) {
            participant.getEvents().removeIf(e -> e.getId().equals(this.getId()));
        }
        return removed;
    }
}
