package com.uqm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    private String description;
    @Valid
    private TaskFlowConfig flowConfig;
}
