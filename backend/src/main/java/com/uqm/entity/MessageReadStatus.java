package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("message_read_status")
public class MessageReadStatus {
    private Integer messageId;
    private Integer userId;
    private Integer isRead;
}
