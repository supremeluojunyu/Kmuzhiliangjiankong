package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.dto.FlowNode;
import com.uqm.dto.NotificationSettingsDto;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.DeadlineRemindLog;
import com.uqm.entity.Message;
import com.uqm.entity.NodeRecord;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskInstance;
import com.uqm.entity.User;
import com.uqm.mapper.DeadlineRemindLogMapper;
import com.uqm.mapper.MessageMapper;
import com.uqm.mapper.MessageTargetGroupMapper;
import com.uqm.mapper.NodeRecordMapper;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.workflow.WorkflowEngine;
import com.uqm.workflow.WorkflowRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {

    private static final List<String> ACTIVE_INSTANCE_STATUS = List.of("pending", "in_progress", "overdue");

    private final SystemConfigService systemConfigService;
    private final TaskDefinitionMapper taskMapper;
    private final TaskInstanceMapper instanceMapper;
    private final NodeRecordMapper nodeRecordMapper;
    private final UserMapper userMapper;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRuntime workflowRuntime;
    private final NotificationService notificationService;
    private final DeadlineRemindLogMapper remindLogMapper;
    private final MessageMapper messageMapper;
    private final MessageTargetGroupMapper messageTargetGroupMapper;

    public String runDeadlineReminders() {
        NotificationSettingsDto cfg = systemConfigService.getNotification();
        if (!cfg.isNotifyOnDeadline()) {
            return "skipped: deadline notification disabled";
        }
        int remindDays = Math.max(1, cfg.getDeadlineRemindDays());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindWindowEnd = now.plusDays(remindDays);

        int sent = 0;
        List<TaskInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .in(TaskInstance::getStatus, ACTIVE_INSTANCE_STATUS));

        for (TaskInstance instance : instances) {
            TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
            if (task == null || !StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
            User user = userMapper.selectById(instance.getAssignedToUserId());
            if (user == null) {
                continue;
            }

            LocalDateTime globalDeadline = parseDateTime(config.getGlobalTimeEnd());
            if (globalDeadline != null && globalDeadline.isAfter(now) && !globalDeadline.isAfter(remindWindowEnd)) {
                String key = "global:" + globalDeadline.toLocalDate();
                if (sendReminder(instance, task, user, key, "任务整体截止提醒",
                        buildGlobalContent(task.getTaskName(), globalDeadline))) {
                    sent++;
                }
            }

            List<NodeRecord> records = nodeRecordMapper.selectList(new LambdaQueryWrapper<NodeRecord>()
                    .eq(NodeRecord::getTaskInstanceId, instance.getId())
                    .in(NodeRecord::getStatus, List.of("in_progress", "draft")));

            for (NodeRecord record : records) {
                FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
                if (node == null || node.getTimeLimitHours() == null || record.getStartTime() == null) {
                    continue;
                }
                LocalDateTime nodeDeadline = record.getStartTime().plusHours(node.getTimeLimitHours());
                if (!nodeDeadline.isAfter(now) || nodeDeadline.isAfter(remindWindowEnd)) {
                    continue;
                }
                String nodeName = StringUtils.hasText(node.getNodeName()) ? node.getNodeName() : node.getNodeId();
                String key = "node:" + node.getNodeId() + ":" + nodeDeadline.toLocalDate();
                if (sendReminder(instance, task, user, key, "节点截止提醒",
                        buildNodeContent(task.getTaskName(), nodeName, nodeDeadline))) {
                    sent++;
                }
            }
        }
        return "sent " + sent + " deadline reminder(s)";
    }

    @Transactional
    public String runOverdueCheck() {
        LocalDateTime now = LocalDateTime.now();
        int marked = 0;

        List<TaskInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .in(TaskInstance::getStatus, List.of("pending", "in_progress")));

        for (TaskInstance instance : instances) {
            TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
            if (task == null || !StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
            if (isOverdue(instance, config, now)) {
                instance.setStatus("overdue");
                instanceMapper.updateById(instance);
                marked++;
            }
        }
        return "marked " + marked + " instance(s) overdue";
    }

    private boolean isOverdue(TaskInstance instance, TaskFlowConfig config, LocalDateTime now) {
        LocalDateTime globalDeadline = parseDateTime(config.getGlobalTimeEnd());
        if (globalDeadline != null && now.isAfter(globalDeadline)) {
            return true;
        }

        List<NodeRecord> records = nodeRecordMapper.selectList(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instance.getId())
                .in(NodeRecord::getStatus, List.of("in_progress", "draft")));

        for (NodeRecord record : records) {
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node == null || node.getTimeLimitHours() == null || record.getStartTime() == null) {
                continue;
            }
            if (now.isAfter(record.getStartTime().plusHours(node.getTimeLimitHours()))) {
                return true;
            }
        }
        return false;
    }

    private boolean sendReminder(TaskInstance instance, TaskDefinition task, User user,
                                 String remindKey, String titlePrefix, String content) {
        if (remindLogMapper.countByInstanceAndKey(instance.getId(), remindKey) > 0) {
            return false;
        }

        String title = "【质量监控】" + titlePrefix + "：" + task.getTaskName();
        notificationService.notifyDeadline(title, content, user.getEmail(), user.getWechatUserId());
        createSystemMessage(instance, task, user, title, content);

        DeadlineRemindLog log = new DeadlineRemindLog();
        log.setInstanceId(instance.getId());
        log.setRemindKey(remindKey);
        log.setUserId(user.getUserId());
        log.setSentAt(LocalDateTime.now());
        remindLogMapper.insert(log);
        return true;
    }

    private void createSystemMessage(TaskInstance instance, TaskDefinition task, User user,
                                       String title, String content) {
        Message message = new Message();
        message.setSenderId(null);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType("system");
        message.setTaskId(task.getTaskId());
        message.setInstanceId(instance.getId());
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        messageTargetGroupMapper.batchInsert(message.getMessageId(), List.of(instance.getTargetGroupId()));
    }

    private String buildGlobalContent(String taskName, LocalDateTime deadline) {
        return "任务「" + taskName + "」将于 " + formatDeadline(deadline) + " 截止，请尽快登录系统处理。";
    }

    private String buildNodeContent(String taskName, String nodeName, LocalDateTime deadline) {
        return "任务「" + taskName + "」的节点「" + nodeName + "」将于 "
                + formatDeadline(deadline) + " 截止，请尽快完成。";
    }

    private String formatDeadline(LocalDateTime deadline) {
        return deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(trimmed.replace(" ", "T"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                log.warn("无法解析截止时间: {}", value);
                return null;
            }
        }
    }
}
