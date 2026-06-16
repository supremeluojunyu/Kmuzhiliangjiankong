package com.uqm.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateGroupRequest {
    private String groupName;
    private String description;
    private Integer parentGroupId;
    private List<Integer> permissionIds;
}
