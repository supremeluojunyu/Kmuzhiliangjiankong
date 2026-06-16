package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupManageVo {
    private Integer groupId;
    private String groupName;
    private String description;
    private Integer parentGroupId;
    private LocalDateTime createdAt;
    private List<Integer> permissionIds;
    private List<String> permissionCodes;
    private long memberCount;
}
