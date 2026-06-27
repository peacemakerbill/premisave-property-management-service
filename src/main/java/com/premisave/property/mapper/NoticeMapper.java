package com.premisave.property.mapper;

import com.premisave.property.dto.request.NoticeRequest;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.entity.Notice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    Notice toEntity(NoticeRequest request);

    NoticeResponse toResponse(Notice notice);
}