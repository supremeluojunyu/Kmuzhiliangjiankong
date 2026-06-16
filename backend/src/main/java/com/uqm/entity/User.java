package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer userId;
    private String name;
    private Integer collegeId;
    private String account;
    private String password;
    private Integer status;
    private String email;
    private String wechatUserId;
    private LocalDateTime createdAt;
}
