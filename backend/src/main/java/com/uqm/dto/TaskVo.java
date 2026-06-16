package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskVo {
    private Integer taskId;
    private String taskName;
    private String description;
    private TaskFlowConfig flowConfig;
    private String status;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
}
