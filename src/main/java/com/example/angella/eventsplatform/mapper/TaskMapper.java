package com.example.angella.eventsplatform.mapper;

import com.example.angella.eventsplatform.entity.Task;
import com.example.angella.eventsplatform.web.dto.TaskDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TaskMapper {
    @Mapping(target = "creator", source = "creator.username")
    @Mapping(target = "assignedUser", source = "assignedUser.username")
    @Mapping(target = "assignedUserId", source = "assignedUser.id")
    TaskDto toDto(Task task);
}