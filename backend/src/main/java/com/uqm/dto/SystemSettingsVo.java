package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemSettingsVo {
    private AuthSettingsDto auth;
    private NotificationSettingsDto notification;
    private RetentionSettingsDto retention;
    private StorageSettingsDto storage;
}
