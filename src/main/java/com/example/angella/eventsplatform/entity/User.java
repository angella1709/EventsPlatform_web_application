package com.example.angella.eventsplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;

    private String firstName;

    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "creator")
    private Set<Event> createdEvents = new HashSet<>();

    @ManyToMany(mappedBy = "participants")
    private Set<Event> events = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "avatar_image_id")
    private Image avatar;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    boolean addEvent(Event event) {
        return events.add(event);
    }

    boolean removeEvent(Event event) {
        return events.removeIf(it -> it.getId().equals(event.getId()));
    }

}