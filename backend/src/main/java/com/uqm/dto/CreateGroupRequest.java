package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "组名称不能为空")
    private String groupName;
    private String description;
    private Integer parentGroupId;
    private List<Integer> permissionIds;
}
