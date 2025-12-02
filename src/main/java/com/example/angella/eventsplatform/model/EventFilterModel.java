package com.example.angella.eventsplatform.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFilterModel {

    @NotNull(message = "Page must be set!")
    @Builder.Default
    private PageModel page = new PageModel(0, 10);

    private Long id;

    private String name;

    private LocalDate startTime;

    private LocalDate endTime;

    private String city;

    private String street;

    private Set<Long> categoryIds;

}
