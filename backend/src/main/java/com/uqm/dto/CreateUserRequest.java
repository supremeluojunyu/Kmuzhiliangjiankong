package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {
    @NotBlank(message = "姓名不能为空")
    private String name;
    @NotBlank(message = "账号不能为空")
    private String account;
    private String password = "admin123";
    @NotNull(message = "学院不能为空")
    private Integer collegeId;
    @NotEmpty(message = "至少分配一个组")
    private List<Integer> groupIds;
    private Integer defaultGroupId;
    private String email;
    private String wechatUserId;
}
