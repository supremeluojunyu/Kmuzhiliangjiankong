package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationLogVo {
    private Long id;
    private Integer userId;
    private String userName;
    private Integer groupId;
    private String action;
    private String targetType;
    private Integer targetId;
    private String detailJson;
    private String ip;
    private LocalDateTime createdAt;
}
