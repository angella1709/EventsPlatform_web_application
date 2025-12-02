package com.example.angella.eventsplatform.mapper;

import com.example.angella.eventsplatform.entity.Category;
import com.example.angella.eventsplatform.entity.Event;
import com.example.angella.eventsplatform.web.dto.CreateEventRequest;
import com.example.angella.eventsplatform.web.dto.EventDto;
import com.example.angella.eventsplatform.web.dto.UpdateEventRequest;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CategoryMapper.class, UserMapper.class}
)
public interface EventMapper {

    @Mapping(target = "description", source = "description")
    @Mapping(target = "location.city", source = "cityLocation")
    @Mapping(target = "location.street", source = "streetLocation")
    @Mapping(target = "creator.id", source = "creatorId")
    @Mapping(target = "startTime", expression = "java(request.getStartTimeAsInstant())")
    @Mapping(target = "endTime", expression = "java(request.getEndTimeAsInstant())")
    Event toEntity(CreateEventRequest request);

    @Mapping(target = "description", source = "description")
    @Mapping(target = "location.city", source = "city")
    @Mapping(target = "location.street", source = "street")
    Event toEntity(UpdateEventRequest request);

    @Mapping(target = "categories", source = "categories")
    @Mapping(target = "creator", source = "creator")
    @Mapping(target = "creator.roles", source = "creator.roles")
    @Mapping(target = "description", source = "description")
    EventDto toDto(Event event);

    List<EventDto> toDtoList(List<Event> events);

    @IterableMapping(qualifiedByName = "mapToCategory")
    Set<Category> mapToCategories(Set<String> categories);

    @Named("mapToCategory")
    default Category mapToCategory(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        Category category = new Category();
        category.setName(categoryName);
        return category;
    }

}