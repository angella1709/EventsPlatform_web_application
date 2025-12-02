package com.example.angella.eventsplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Название мероприятия должно быть указано!")
    private String name;

    @NotBlank(message = "Время начала должно быть указано!")
    private String startTime;  // ДОЛЖНО БЫТЬ String!

    @NotBlank(message = "Время окончания должно быть указано!")
    private String endTime;    // ДОЛЖНО БЫТЬ String!

    @NotEmpty(message = "Выберите хотя бы одну категорию!")
    private Set<@NotBlank(message = "Категория не может быть пустой!") String> categories;

    @NotBlank(message = "Описание мероприятия должно быть указано!")
    private String description;

    @NotBlank(message = "Город должен быть указан!")
    private String cityLocation;

    @NotBlank(message = "Улица должна быть указана!")
    private String streetLocation;

    @NotNull(message = "ID создателя должен быть указан!")
    private Long creatorId;

    // Методы для преобразования в Instant с секундами = 00
    public Instant getStartTimeAsInstant() {
        if (startTime == null || startTime.isEmpty()) {
            return null;
        }
        // Добавляем секунды :00 если их нет
        String timeWithSeconds = startTime.length() == 16 ? startTime + ":00" : startTime;
        LocalDateTime localDateTime = LocalDateTime.parse(timeWithSeconds, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public Instant getEndTimeAsInstant() {
        if (endTime == null || endTime.isEmpty()) {
            return null;
        }
        // Добавляем секунды :00 если их нет
        String timeWithSeconds = endTime.length() == 16 ? endTime + ":00" : endTime;
        LocalDateTime localDateTime = LocalDateTime.parse(timeWithSeconds, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}