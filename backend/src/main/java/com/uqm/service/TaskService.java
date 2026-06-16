package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
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

    public PageResult<TaskVo> listTasks(LoginUser user, long page, long pageSize, String status) {
        LambdaQueryWrapper<TaskDefinition> wrapper = new LambdaQueryWrapper<TaskDefinition>()
                .orderByDesc(TaskDefinition::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TaskDefinition::getStatus, status);
        }
        Page<TaskDefinition> result = taskMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    public TaskVo getTask(Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return toVo(task);
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
        return toVo(task);
    }

    @Transactional
    public TaskVo updateTask(LoginUser user, Integer taskId, CreateTaskRequest request) {
        permissionService.requirePermission(user, "task:config");
        TaskDefinition task = requireEditable(taskId);

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
        return toVo(task);
    }

    @Transactional
    public TaskVo publishTask(LoginUser user, Integer taskId) {
        permissionService.requirePermission(user, "task:create");
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
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

        return toVo(task);
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
        return toVo(task);
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
        return toVo(task);
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
        return toVo(task);
    }

    private TaskDefinition requireTask(Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private TaskDefinition requireEditable(Integer taskId) {
        TaskDefinition task = requireTask(taskId);
        if (!List.of("draft", "paused").contains(task.getStatus())) {
            throw new BusinessException(400, "仅草稿或已暂停的任务可编辑配置");
        }
        return task;
    }

    private TaskDefinition requireDraft(Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!"draft".equals(task.getStatus())) {
            throw new BusinessException(400, "仅草稿状态任务可编辑");
        }
        return task;
    }

    private TaskVo toVo(TaskDefinition task) {
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
                .build();
    }
}
