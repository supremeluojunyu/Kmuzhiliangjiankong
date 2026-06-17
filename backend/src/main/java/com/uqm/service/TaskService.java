package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uqm.common.BusinessException;
import com.uqm.common.DeleteConfirmSupport;
import com.uqm.common.PageResult;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateTaskRequest;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.dto.TaskVo;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.User;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.HtmlSanitizer;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskDefinitionMapper taskMapper;
    private final UserMapper userMapper;
    private final WorkflowEngine workflowEngine;
    private final PermissionService permissionService;
    private final HtmlSanitizer htmlSanitizer;
    private final TaskPublishNotifier taskPublishNotifier;
    private final TaskFlowSyncService taskFlowSyncService;
    private final OperationLogService operationLogService;
    private final ResourceCascadeService resourceCascadeService;
    private final AdminGuardService adminGuardService;

    public PageResult<TaskVo> listTasks(LoginUser user, long page, long pageSize, String status) {
        requireTaskListAccess(user);

        LambdaQueryWrapper<TaskDefinition> wrapper = new LambdaQueryWrapper<TaskDefinition>()
                .orderByDesc(TaskDefinition::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TaskDefinition::getStatus, status);
        }
        if (!canViewAllTasks(user)) {
            wrapper.eq(TaskDefinition::getCreatorId, user.getUserId());
        }

        Page<TaskDefinition> result = taskMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(t -> toVo(t, user)).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    public TaskVo getTask(LoginUser user, Integer taskId) {
        TaskDefinition task = requireTask(taskId);
        requireViewPermission(user, task);
        return toVo(task, user);
    }

    @Transactional
    public TaskVo createTask(LoginUser user, CreateTaskRequest request) {
        permissionService.requirePermission(user, "task:create");

        TaskDefinition task = new TaskDefinition();
        task.setTaskName(request.getTaskName());
        task.setDescription(htmlSanitizer.sanitize(request.getDescription()));
        task.setStatus("draft");
        task.setCreatorId(user.getUserId());

        if (request.getFlowConfig() != null) {
            workflowEngine.validate(request.getFlowConfig());
            request.getFlowConfig().setTaskId(null);
            task.setConfigJson(workflowEngine.toJson(request.getFlowConfig()));
        }

        taskMapper.insert(task);
        operationLogService.log(user, "task:create", "task", task.getTaskId(), null);
        return toVo(task, user);
    }

    @Transactional
    public TaskVo updateTask(LoginUser user, Integer taskId, CreateTaskRequest request) {
        TaskDefinition task = requireTask(taskId);
        requireEditPermission(user, task);

        task.setTaskName(request.getTaskName());
        task.setDescription(htmlSanitizer.sanitize(request.getDescription()));
        if (request.getFlowConfig() != null) {
            workflowEngine.validate(request.getFlowConfig());
            request.getFlowConfig().setTaskId(taskId);
            task.setConfigJson(workflowEngine.toJson(request.getFlowConfig()));
            if ("paused".equals(task.getStatus())) {
                taskFlowSyncService.syncActiveInstances(taskId, request.getFlowConfig());
            }
        }
        taskMapper.updateById(task);
        operationLogService.log(user, "task:update", "task", taskId, null);
        return toVo(task, user);
    }

    @Transactional
    public TaskVo publishTask(LoginUser user, Integer taskId) {
        permissionService.requirePermission(user, "task:create");
        TaskDefinition task = requireTask(taskId);
        requireCreatorOrManage(user, task, "仅任务创建者或管理员可发布该任务");
        if (!"draft".equals(task.getStatus())) {
            throw new BusinessException(400, "仅草稿状态任务可发布");
        }
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        workflowEngine.validate(config);
        config.setTaskId(taskId);
        task.setConfigJson(workflowEngine.toJson(config));
        task.setStatus("published");
        taskMapper.updateById(task);

        taskPublishNotifier.notifyTaskPublished(task);
        operationLogService.log(user, "task:publish", "task", taskId, null);

        return toVo(task, user);
    }

    @Transactional
    public TaskVo pauseTask(LoginUser user, Integer taskId) {
        permissionService.requirePermission(user, "task:config");
        TaskDefinition task = requireTask(taskId);
        if (!List.of("published", "in_progress").contains(task.getStatus())) {
            throw new BusinessException(400, "仅已发布或进行中的任务可暂停");
        }
        task.setStatus("paused");
        taskMapper.updateById(task);
        operationLogService.log(user, "task:pause", "task", taskId, null);
        return toVo(task, user);
    }

    @Transactional
    public TaskVo resumeTask(LoginUser user, Integer taskId) {
        permissionService.requirePermission(user, "task:config");
        TaskDefinition task = requireTask(taskId);
        if (!"paused".equals(task.getStatus())) {
            throw new BusinessException(400, "仅已暂停的任务可恢复运行");
        }
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        workflowEngine.validate(config);
        task.setStatus(taskFlowSyncService.hasActiveInstances(taskId) ? "in_progress" : "published");
        taskMapper.updateById(task);
        operationLogService.log(user, "task:resume", "task", taskId, null);
        return toVo(task, user);
    }

    @Transactional
    public TaskVo stopTask(LoginUser user, Integer taskId) {
        permissionService.requirePermission(user, "task:config");
        TaskDefinition task = requireTask(taskId);
        if (!List.of("published", "in_progress", "paused").contains(task.getStatus())) {
            throw new BusinessException(400, "当前状态的任务不可停止");
        }
        taskFlowSyncService.closeActiveInstances(taskId);
        task.setStatus("closed");
        taskMapper.updateById(task);
        operationLogService.log(user, "task:stop", "task", taskId, null);
        return toVo(task, user);
    }

    private void requireTaskListAccess(LoginUser user) {
        if (!permissionService.hasPermission(user, "task:create")
                && !permissionService.hasPermission(user, "task:config")) {
            throw new BusinessException(403, "无任务管理权限");
        }
    }

    private boolean canViewAllTasks(LoginUser user) {
        return permissionService.hasPermission(user, "task:config")
                || permissionService.hasPermission(user, "stat:view_all")
                || permissionService.hasPermission(user, "system:config");
    }

    private void requireViewPermission(LoginUser user, TaskDefinition task) {
        if (canViewAllTasks(user)) {
            return;
        }
        if (permissionService.hasPermission(user, "task:create")
                && user.getUserId().equals(task.getCreatorId())) {
            return;
        }
        throw new BusinessException(403, "无权查看该任务");
    }

    private void requireEditPermission(LoginUser user, TaskDefinition task) {
        if (!List.of("draft", "paused").contains(task.getStatus())) {
            throw new BusinessException(400, "仅草稿或已暂停的任务可编辑，请先暂停后再修改");
        }
        if (permissionService.hasPermission(user, "task:config")) {
            return;
        }
        if (permissionService.hasPermission(user, "task:create")
                && user.getUserId().equals(task.getCreatorId())) {
            return;
        }
        throw new BusinessException(403, "无权编辑该任务");
    }

    private void requireCreatorOrManage(LoginUser user, TaskDefinition task, String message) {
        if (permissionService.hasPermission(user, "task:config")) {
            return;
        }
        if (user.getUserId().equals(task.getCreatorId())) {
            return;
        }
        throw new BusinessException(403, message);
    }

    private boolean canEditTask(LoginUser user, TaskDefinition task) {
        if (!List.of("draft", "paused").contains(task.getStatus())) {
            return false;
        }
        if (permissionService.hasPermission(user, "task:config")) {
            return true;
        }
        return permissionService.hasPermission(user, "task:create")
                && user.getUserId().equals(task.getCreatorId());
    }

    private boolean canManageTask(LoginUser user) {
        return permissionService.hasPermission(user, "task:config");
    }

    @Transactional
    public BatchDeleteResultVo deleteTasks(LoginUser user, ConfirmDeleteRequest request) {
        DeleteConfirmSupport.validate(request.getConfirmPhrase());
        permissionService.requirePermission(user, "task:config");

        List<String> errors = new ArrayList<>();
        int deleted = 0;
        for (Integer taskId : request.getIds()) {
            try {
                deleteSingleTask(user, taskId);
                deleted++;
            } catch (BusinessException e) {
                errors.add("任务#" + taskId + "：" + e.getMessage());
            }
        }
        return BatchDeleteResultVo.builder().deletedCount(deleted).errors(errors).build();
    }

    private void deleteSingleTask(LoginUser user, Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        boolean force = adminGuardService.canForceCascadeDelete(user);
        if (!force && !resourceCascadeService.isTaskDeletable(task)) {
            throw new BusinessException(400, "任务进行中或已发布，请先暂停或停止后再删除");
        }
        resourceCascadeService.cascadeDeleteTask(user, taskId, force);
    }

    private TaskDefinition requireTask(Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private TaskVo toVo(TaskDefinition task, LoginUser user) {
        TaskFlowConfig flowConfig = workflowEngine.parseJson(task.getConfigJson());
        String creatorName = null;
        if (task.getCreatorId() != null) {
            User creator = userMapper.selectById(task.getCreatorId());
            creatorName = creator != null ? creator.getName() : null;
        }
        return TaskVo.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .flowConfig(flowConfig)
                .status(task.getStatus())
                .creatorId(task.getCreatorId())
                .creatorName(creatorName)
                .createdAt(task.getCreatedAt())
                .editable(canEditTask(user, task))
                .canManage(canManageTask(user))
                .deletable(adminGuardService.canForceCascadeDelete(user)
                        || resourceCascadeService.isTaskDeletable(task))
                .build();
    }
}
