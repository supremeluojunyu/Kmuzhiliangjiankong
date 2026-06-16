package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("message_target_group")
public class MessageTargetGroup {
    private Integer messageId;
    private Integer groupId;
}
