package com.example.angella.eventsplatform.mapper;

import com.example.angella.eventsplatform.entity.ChatMessage;
import com.example.angella.eventsplatform.web.dto.ChatMessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ImageMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ChatMessageMapper {

    @Mapping(target = "author", source = "author.username")
    @Mapping(target = "images", source = "images")
    ChatMessageDto toDto(ChatMessage chatMessage);
}