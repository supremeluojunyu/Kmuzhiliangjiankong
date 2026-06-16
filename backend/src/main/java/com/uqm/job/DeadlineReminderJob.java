package com.uqm.job;

import com.uqm.service.DeadlineReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineReminderJob {

    private final DeadlineReminderService deadlineReminderService;

    @Scheduled(cron = "0 0 8 * * *")
    public void dailyDeadlineRemind() {
        try {
            String result = deadlineReminderService.runDeadlineReminders();
            if (!result.startsWith("skipped")) {
                log.info("截止提醒: {}", result);
            }
        } catch (Exception e) {
            log.warn("截止提醒任务失败: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 * * * *")
    public void hourlyOverdueCheck() {
        try {
            String result = deadlineReminderService.runOverdueCheck();
            if (!result.endsWith("0 instance(s) overdue")) {
                log.info("逾期检测: {}", result);
            }
        } catch (Exception e) {
            log.warn("逾期检测任务失败: {}", e.getMessage());
        }
    }
}
