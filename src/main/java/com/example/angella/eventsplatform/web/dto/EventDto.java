package com.example.angella.eventsplatform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private Long id;

    private String name;

    private Instant startTime;

    private Instant endTime;

    private Set<CategoryDto> categories = new HashSet<>();

    private String description;

    private LocationDto location;

    private UserDto creator;

}
