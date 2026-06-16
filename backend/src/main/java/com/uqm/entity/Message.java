package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Integer messageId;
    private Integer senderId;
    private String title;
    private String content;
    private String messageType;
    private Integer taskId;
    private Integer instanceId;
    private LocalDateTime sendTime;
}
