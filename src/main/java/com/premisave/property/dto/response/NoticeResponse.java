package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeResponse {
    private String id;
    private String title;
    private String content;
    private LocalDateTime sentAt;
}