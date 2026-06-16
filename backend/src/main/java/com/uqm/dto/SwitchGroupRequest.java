package com.uqm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SwitchGroupRequest {
    @NotNull(message = "组ID不能为空")
    private Integer groupId;
}
