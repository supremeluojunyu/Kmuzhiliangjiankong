package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Integer userId;
    private String name;
    private Integer currentGroupId;
    private String currentGroupName;
    private List<String> permissions;
    private List<GroupDto> groups;
}
