package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageVo {
    private Integer messageId;
    private Integer senderId;
    private String senderName;
    private String title;
    private String content;
    private String messageType;
    private Integer taskId;
    private Integer instanceId;
    private LocalDateTime sendTime;
    private Boolean isRead;
    /** 是否为当前用户发出的消息 */
    private Boolean sentByMe;
    private List<String> targetGroupNames;
    private List<String> targetUserNames;
}
