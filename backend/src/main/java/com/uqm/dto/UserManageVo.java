package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserManageVo {
    private Integer userId;
    private String name;
    private String account;
    private Integer collegeId;
    private String collegeName;
    private Integer status;
    private LocalDateTime createdAt;
    private List<GroupBriefVo> groups;
    private Integer defaultGroupId;
    private String email;
    private String wechatUserId;
}
