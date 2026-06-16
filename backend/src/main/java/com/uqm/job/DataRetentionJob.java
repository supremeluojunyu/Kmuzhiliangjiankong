package com.uqm.job;

import com.uqm.service.DataRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionJob {

    private final DataRetentionService dataRetentionService;

    @Scheduled(cron = "0 0 * * * *")
    public void hourlyCheck() {
        try {
            String result = dataRetentionService.runScheduledCleanup();
            if (!result.startsWith("skipped")) {
                log.info("定时数据保留: {}", result);
            }
        } catch (Exception e) {
            log.warn("数据保留任务失败: {}", e.getMessage());
        }
    }
}
