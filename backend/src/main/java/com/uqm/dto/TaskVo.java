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
    /** 当前用户是否可编辑（草稿/已暂停） */
    private Boolean editable;
    /** 当前用户是否可暂停/恢复/停止（需 task:config） */
    private Boolean canManage;
    /** 是否可删除（草稿/已暂停/已停止） */
    private Boolean deletable;
}
