package com.uqm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageRow {
    private Integer messageId;
    private Integer senderId;
    private String title;
    private String content;
    private String messageType;
    private Integer taskId;
    private Integer instanceId;
    private LocalDateTime sendTime;
    private String senderName;
    private Integer isRead;
}
