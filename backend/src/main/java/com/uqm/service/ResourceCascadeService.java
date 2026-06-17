package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.DeadlineRemindLog;
import com.uqm.entity.Message;
import com.uqm.entity.MessageReadStatus;
import com.uqm.entity.MessageTargetGroup;
import com.uqm.entity.MessageTargetUser;
import com.uqm.entity.NodeRecord;
import com.uqm.entity.TaskAllocation;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskInstance;
import com.uqm.entity.TaskTemplate;
import com.uqm.mapper.DeadlineRemindLogMapper;
import com.uqm.mapper.MessageMapper;
import com.uqm.mapper.MessageReadStatusMapper;
import com.uqm.mapper.MessageTargetGroupMapper;
import com.uqm.mapper.MessageTargetUserMapper;
import com.uqm.mapper.NodeRecordMapper;
import com.uqm.mapper.TaskAllocationMapper;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.mapper.TaskMaintenanceMapper;
import com.uqm.mapper.TaskTemplateMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.security.LoginUser;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一删除策略：仅当关联任务均为草稿/已暂停/已停止时可删；删除时级联清理全部关联数据。
 */
@Service
@RequiredArgsConstructor
public class ResourceCascadeService {

    static final Set<String> DELETABLE_TASK_STATUSES = Set.of("draft", "paused", "closed");
    static final Set<String> ACTIVE_TASK_STATUSES = Set.of("published", "in_progress");

    private final TaskDefinitionMapper taskMapper;
    private final TaskInstanceMapper instanceMapper;
    private final TaskTemplateMapper templateMapper;
    private final NodeRecordMapper nodeRecordMapper;
    private final TaskAllocationMapper allocationMapper;
    private final TaskMaintenanceMapper taskMaintenanceMapper;
    private final DeadlineRemindLogMapper deadlineRemindLogMapper;
    private final MessageMapper messageMapper;
    private final MessageReadStatusMapper readStatusMapper;
    private final MessageTargetGroupMapper targetGroupMapper;
    private final MessageTargetUserMapper targetUserMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final WorkflowEngine workflowEngine;
    private final OperationLogService operationLogService;

    public boolean isTaskDeletable(TaskDefinition task) {
        return task != null && DELETABLE_TASK_STATUSES.contains(task.getStatus());
    }

    public void assertTaskDeletable(TaskDefinition task) {
        if (!isTaskDeletable(task)) {
            throw new BusinessException(400, "任务「" + task.getTaskName() + "」进行中或已发布，请先暂停或停止后再删除");
        }
    }

    public void assertUserDeletable(Integer userId) {
        List<TaskDefinition> created = taskMapper.selectList(new LambdaQueryWrapper<TaskDefinition>()
                .eq(TaskDefinition::getCreatorId, userId));
        for (TaskDefinition task : created) {
            if (ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
                throw new BusinessException(400, "该用户创建的任务「" + task.getTaskName() + "」仍在进行中，请先暂停或停止");
            }
        }

        List<TaskInstance> assigned = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getAssignedToUserId, userId));
        for (TaskInstance instance : assigned) {
            TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
            if (task != null && ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
                throw new BusinessException(400, "该用户参与的任务「" + task.getTaskName() + "」仍在进行中，请先暂停或停止");
            }
        }
    }

    public void assertGroupDeletable(Integer groupId) {
        for (TaskDefinition task : findTasksRelatedToGroup(groupId)) {
            if (ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
                throw new BusinessException(400, "组关联的任务「" + task.getTaskName() + "」仍在进行中，请先暂停或停止");
            }
        }
    }

    public boolean isUserDeletable(Integer userId) {
        try {
            assertUserDeletable(userId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public boolean isGroupDeletable(Integer groupId) {
        try {
            assertGroupDeletable(groupId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public Set<TaskDefinition> findTasksRelatedToGroup(Integer groupId) {
        Set<Integer> taskIds = new HashSet<>();
        List<TaskDefinition> allTasks = taskMapper.selectList(null);
        for (TaskDefinition task : allTasks) {
            TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
            if (config != null && config.getNodes() != null) {
                boolean usesGroup = config.getNodes().stream()
                        .anyMatch(n -> groupId.equals(n.getExecuteGroupId()));
                if (usesGroup) {
                    taskIds.add(task.getTaskId());
                }
            }
        }
        instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                        .eq(TaskInstance::getTargetGroupId, groupId))
                .forEach(i -> taskIds.add(i.getTaskDefinitionId()));

        if (taskIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(taskMapper.selectBatchIds(taskIds));
    }

    @Transactional
    public void cascadeDeleteTask(LoginUser operator, Integer taskId, boolean force) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        if (!force) {
            assertTaskDeletable(task);
        }

        List<TaskInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId));
        for (TaskInstance instance : instances) {
            cascadeDeleteInstance(instance.getId());
        }

        instanceMapper.delete(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId));
        allocationMapper.delete(new LambdaQueryWrapper<TaskAllocation>()
                .eq(TaskAllocation::getTaskId, taskId));
        taskMaintenanceMapper.deleteAttachmentsByTaskId(taskId);
        taskMaintenanceMapper.clearMessageTaskReference(taskId);
        taskMapper.deleteById(taskId);

        if (operator != null) {
            operationLogService.log(operator, "task:delete", "task", taskId,
                    Map.of("taskName", task.getTaskName()));
        }
    }

    @Transactional
    public void cascadeDeleteInstance(Integer instanceId) {
        purgeMessagesForInstance(instanceId);
        deadlineRemindLogMapper.delete(new LambdaQueryWrapper<DeadlineRemindLog>()
                .eq(DeadlineRemindLog::getInstanceId, instanceId));
        nodeRecordMapper.delete(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instanceId));
        taskMaintenanceMapper.clearMessageInstanceReference(instanceId);
        instanceMapper.deleteById(instanceId);
    }

    @Transactional
    public void cascadeDeleteTemplate(Integer templateId) {
        templateMapper.deleteById(templateId);
    }

    @Transactional
    public void cascadeDeleteUserData(LoginUser operator, Integer userId, boolean force) {
        if (!force) {
            assertUserDeletable(userId);
        }

        List<TaskDefinition> createdTasks = taskMapper.selectList(new LambdaQueryWrapper<TaskDefinition>()
                .eq(TaskDefinition::getCreatorId, userId));
        for (TaskDefinition task : createdTasks) {
            cascadeDeleteTask(operator, task.getTaskId(), force);
        }

        List<TaskInstance> remaining = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getAssignedToUserId, userId));
        for (TaskInstance instance : remaining) {
            cascadeDeleteInstance(instance.getId());
        }

        templateMapper.delete(new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getCreatorId, userId));

        List<Message> sentMessages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, userId));
        for (Message message : sentMessages) {
            purgeMessage(message.getMessageId());
        }

        deadlineRemindLogMapper.delete(new LambdaQueryWrapper<DeadlineRemindLog>()
                .eq(DeadlineRemindLog::getUserId, userId));
        readStatusMapper.delete(new LambdaQueryWrapper<MessageReadStatus>()
                .eq(MessageReadStatus::getUserId, userId));
        targetUserMapper.delete(new LambdaQueryWrapper<MessageTargetUser>()
                .eq(MessageTargetUser::getUserId, userId));
        taskMaintenanceMapper.clearSystemConfigUpdater(userId);
    }

    @Transactional
    public void cascadeDeleteGroupData(LoginUser operator, Integer groupId, boolean force) {
        if (!force) {
            assertGroupDeletable(groupId);
        }

        Set<TaskDefinition> related = findTasksRelatedToGroup(groupId);
        for (TaskDefinition task : related) {
            cascadeDeleteTask(operator, task.getTaskId(), force);
        }

        targetGroupMapper.delete(new LambdaQueryWrapper<MessageTargetGroup>()
                .eq(MessageTargetGroup::getGroupId, groupId));
        userGroupRelationMapper.deleteByGroupId(groupId);
    }

    private void purgeMessagesForInstance(Integer instanceId) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getInstanceId, instanceId));
        for (Message message : messages) {
            purgeMessage(message.getMessageId());
        }
    }

    private void purgeMessage(Integer messageId) {
        readStatusMapper.delete(new LambdaQueryWrapper<MessageReadStatus>()
                .eq(MessageReadStatus::getMessageId, messageId));
        targetGroupMapper.delete(new LambdaQueryWrapper<MessageTargetGroup>()
                .eq(MessageTargetGroup::getMessageId, messageId));
        targetUserMapper.delete(new LambdaQueryWrapper<MessageTargetUser>()
                .eq(MessageTargetUser::getMessageId, messageId));
        messageMapper.deleteById(messageId);
    }
}
