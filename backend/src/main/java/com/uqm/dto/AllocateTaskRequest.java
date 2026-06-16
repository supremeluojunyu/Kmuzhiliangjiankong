package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AllocateTaskRequest {
    @NotNull(message = "任务ID不能为空")
    private Integer taskId;
    @NotBlank(message = "分配类型不能为空")
    private String allocationType;
    @NotNull(message = "目标组不能为空")
    private Integer targetGroupId;
    private List<Integer> collegeIds;
    private Integer totalInstances;
}
