package com.uqm.dto;

import lombok.Data;

@Data
public class GroupDto {
    private Integer groupId;
    private String groupName;
    private Integer isDefault;
    private Long pendingCount;
}
