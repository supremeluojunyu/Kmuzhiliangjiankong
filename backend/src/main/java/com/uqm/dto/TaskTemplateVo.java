package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskTemplateVo {
    private Integer templateId;
    private String templateName;
    private String description;
    private TaskFlowConfig flowConfig;
    private Integer creatorId;
    private String creatorName;
    private Integer nodeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
