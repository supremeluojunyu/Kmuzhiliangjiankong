package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupBriefVo {
    private Integer groupId;
    private String groupName;
}
