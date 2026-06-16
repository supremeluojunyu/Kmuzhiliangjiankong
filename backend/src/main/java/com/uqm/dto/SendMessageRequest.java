package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String content;
    private String messageType = "broadcast";
    private Integer taskId;
    private Integer instanceId;
    @NotEmpty(message = "至少选择一个目标组")
    private List<Integer> targetGroupIds;
}
