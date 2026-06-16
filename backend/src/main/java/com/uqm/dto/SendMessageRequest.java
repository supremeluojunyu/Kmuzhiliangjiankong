package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
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
    /** 选中组时：组内全员接收 */
    private List<Integer> targetGroupIds;
    /** 选中个人时：仅该用户接收 */
    private List<Integer> targetUserIds;
}
