package com.uqm.service;

import com.uqm.dto.FlowNode;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.User;
import com.uqm.mapper.UserGroupQueryMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskPublishNotifier {

    private final NotificationService notificationService;
    private final SystemConfigService systemConfigService;
    private final UserGroupQueryMapper userGroupQueryMapper;
    private final UserMapper userMapper;
    private final WorkflowEngine workflowEngine;

    public void notifyTaskPublished(TaskDefinition task) {
        if (!systemConfigService.getNotification().isNotifyOnTaskPublish()) {
            return;
        }
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        if (config.getNodes() == null) {
            return;
        }
        Set<Integer> groupIds = new HashSet<>();
        for (FlowNode node : config.getNodes()) {
            if (node.getExecuteGroupId() != null) {
                groupIds.add(node.getExecuteGroupId());
            }
        }
        Set<Integer> notified = new HashSet<>();
        for (Integer groupId : groupIds) {
            userGroupQueryMapper.listUsersInGroup(groupId, null).forEach(item -> {
                if (!notified.add(item.getUserId())) {
                    return;
                }
                User user = userMapper.selectById(item.getUserId());
                if (user == null) {
                    return;
                }
                String email = StringUtils.hasText(user.getEmail()) ? user.getEmail() : null;
                notificationService.notifyTaskPublished(task.getTaskName(), email, user.getWechatUserId());
            });
        }
    }
}
