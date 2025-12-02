package com.example.angella.eventsplatform.mapper;

import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.web.dto.CreateUserRequest;
import com.example.angella.eventsplatform.web.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    User toEntity(CreateUserRequest request);

    UserDto toDto(User user);
}
