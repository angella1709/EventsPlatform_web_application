package com.example.angella.eventsplatform.mapper;

import com.example.angella.eventsplatform.entity.Comment;
import com.example.angella.eventsplatform.web.dto.CommentDto;
import com.example.angella.eventsplatform.web.dto.CreateCommentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CommentMapper {

    Comment toEntity(CreateCommentRequest request);

    @Mapping(target = "author", source = "user.username")
    CommentDto toDto(Comment comment);

    List<CommentDto> toDtoList(List<Comment> comments);

}
