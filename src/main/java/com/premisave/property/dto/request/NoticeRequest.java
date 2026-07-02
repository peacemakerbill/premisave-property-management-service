package com.premisave.property.dto.request;

import com.premisave.property.enums.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoticeRequest {

    @NotBlank
    private String tenantId;

    private String leaseId;

    @NotNull
    private NoticeType noticeType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}