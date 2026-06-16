package com.uqm.dto;

import lombok.Data;

@Data
public class NotificationSettingsDto {
    private boolean emailEnabled;
    private String smtpHost;
    private int smtpPort = 587;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpFrom;
    private boolean smtpSsl = true;
    private boolean wechatEnabled;
    private String wechatCorpId;
    private String wechatAgentId;
    private String wechatSecret;
    private boolean notifyOnTaskPublish = true;
    private boolean notifyOnMessageBroadcast;
    private boolean notifyOnDeadline;
    private int deadlineRemindDays = 3;
}
