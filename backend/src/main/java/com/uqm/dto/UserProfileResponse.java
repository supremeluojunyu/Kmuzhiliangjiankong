package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    private Integer userId;
    private String name;
    private String account;
    private Integer collegeId;
    private String collegeName;
    private Integer currentGroupId;
    private String currentGroupName;
    private List<String> permissions;
    private List<GroupDto> groups;
}
