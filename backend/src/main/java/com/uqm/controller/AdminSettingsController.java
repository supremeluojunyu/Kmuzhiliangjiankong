package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.SystemSettingsVo;
import com.uqm.dto.TestNotificationRequest;
import com.uqm.security.LoginUser;
import com.uqm.service.DataRetentionService;
import com.uqm.service.DeadlineReminderService;
import com.uqm.service.NotificationService;
import com.uqm.service.SystemConfigService;
import com.uqm.storage.DynamicStorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SystemConfigService systemConfigService;
    private final NotificationService notificationService;
    private final DataRetentionService dataRetentionService;
    private final DeadlineReminderService deadlineReminderService;
    private final DynamicStorageProvider dynamicStorageProvider;

    @GetMapping
    public ApiResponse<SystemSettingsVo> get(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(systemConfigService.getSettings(user));
    }

    @PutMapping
    public ApiResponse<SystemSettingsVo> update(@AuthenticationPrincipal LoginUser user,
                                                @RequestBody SystemSettingsVo body) {
        SystemSettingsVo updated = systemConfigService.updateSettings(user, body);
        if (body.getStorage() != null) {
            dynamicStorageProvider.invalidate();
        }
        return ApiResponse.ok(updated);
    }

    @PostMapping("/test-notification")
    public ApiResponse<Void> testNotification(@AuthenticationPrincipal LoginUser user,
                                              @RequestBody TestNotificationRequest request) {
        systemConfigService.getSettings(user);
        notificationService.testNotification(request.getChannel(), request.getTarget());
        return ApiResponse.ok(null);
    }

    @PostMapping("/retention/run")
    public ApiResponse<String> runRetention(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(dataRetentionService.runCleanup(user));
    }

    @PostMapping("/deadline/remind")
    public ApiResponse<String> runDeadlineRemind(@AuthenticationPrincipal LoginUser user) {
        systemConfigService.getSettings(user);
        return ApiResponse.ok(deadlineReminderService.runDeadlineReminders());
    }

    @PostMapping("/deadline/overdue-check")
    public ApiResponse<String> runOverdueCheck(@AuthenticationPrincipal LoginUser user) {
        systemConfigService.getSettings(user);
        return ApiResponse.ok(deadlineReminderService.runOverdueCheck());
    }

    @PostMapping("/storage/test")
    public ApiResponse<String> testStorage(@AuthenticationPrincipal LoginUser user) {
        systemConfigService.getSettings(user);
        dynamicStorageProvider.testConnection();
        return ApiResponse.ok("存储连接测试成功");
    }
}
