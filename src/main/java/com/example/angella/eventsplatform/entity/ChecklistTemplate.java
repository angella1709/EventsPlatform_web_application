package com.example.angella.eventsplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private TemplateCategory category;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "template")
    private Set<TemplateItem> items = new HashSet<>();
}

