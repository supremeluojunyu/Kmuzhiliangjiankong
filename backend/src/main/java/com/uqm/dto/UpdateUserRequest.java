package com.uqm.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String name;
    private Integer collegeId;
    private Integer status;
    private List<Integer> groupIds;
    private Integer defaultGroupId;
    private String password;
    private String email;
    private String wechatUserId;
}
