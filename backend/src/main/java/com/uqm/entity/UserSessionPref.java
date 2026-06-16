package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_session_pref")
public class UserSessionPref {

    @TableId
    private Integer userId;
    private Integer currentGroupId;
    private LocalDateTime updatedAt;
}
