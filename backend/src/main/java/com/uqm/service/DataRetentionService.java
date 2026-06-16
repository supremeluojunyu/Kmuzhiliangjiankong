package com.uqm.service;

import com.uqm.dto.RetentionSettingsDto;
import com.uqm.mapper.RetentionMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private final SystemConfigService systemConfigService;
    private final RetentionMapper retentionMapper;

    @Transactional
    public String runCleanup(LoginUser operator) {
        RetentionSettingsDto cfg = systemConfigService.getRetention();
        if (!cfg.isEnabled()) {
            return "数据保留未启用";
        }
        return executeCleanup(cfg, operator != null ? operator.getUserId() : null);
    }

    @Transactional
    public String runScheduledCleanup() {
        RetentionSettingsDto cfg = systemConfigService.getRetention();
        if (!cfg.isEnabled()) {
            return "skipped";
        }
        if (LocalDateTime.now().getHour() != cfg.getRunHour()) {
            return "skipped-hour";
        }
        return executeCleanup(cfg, null);
    }

    private String executeCleanup(RetentionSettingsDto cfg, Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        int logs = retentionMapper.deleteLogsBefore(now.minusYears(cfg.getLogDataYears()));
        int msgRead = retentionMapper.deleteMessageReadBefore(now.minusYears(cfg.getMessageDataYears()));
        int msgTarget = retentionMapper.deleteMessageTargetsBefore(now.minusYears(cfg.getMessageDataYears()));
        int msgTargetUser = retentionMapper.deleteMessageTargetUsersBefore(now.minusYears(cfg.getMessageDataYears()));
        int msgs = retentionMapper.deleteMessagesBefore(now.minusYears(cfg.getMessageDataYears()));
        int nodes = retentionMapper.deleteNodeRecordsBefore(now.minusYears(cfg.getTaskDataYears()));
        int instances = retentionMapper.deleteTaskInstancesBefore(now.minusYears(cfg.getTaskDataYears()));

        String summary = String.format("清理完成：日志 %d，消息 %d，节点记录 %d，任务实例 %d",
                logs, msgs, nodes, instances);
        cfg.setLastRunAt(now);
        cfg.setLastRunSummary(summary + String.format("（关联已读 %d，目标组 %d，目标用户 %d）", msgRead, msgTarget, msgTargetUser));
        systemConfigService.saveRetention(cfg, userId);
        log.info(summary);
        return summary;
    }
}
