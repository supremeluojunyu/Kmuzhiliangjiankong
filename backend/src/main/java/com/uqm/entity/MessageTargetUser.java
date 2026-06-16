package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("message_target_user")
public class MessageTargetUser {
    private Integer messageId;
    private Integer userId;
}
