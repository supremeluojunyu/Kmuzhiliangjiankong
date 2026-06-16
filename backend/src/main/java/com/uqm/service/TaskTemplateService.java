package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.CreateFromTemplateRequest;
import com.uqm.dto.CreateTaskRequest;
import com.uqm.dto.SaveTemplateRequest;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.dto.TaskTemplateVo;
import com.uqm.dto.TaskVo;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskTemplate;
import com.uqm.entity.User;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskTemplateMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.HtmlSanitizer;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskTemplateService {

    private final TaskTemplateMapper templateMapper;
    private final TaskDefinitionMapper taskMapper;
    private final UserMapper userMapper;
    private final TaskService taskService;
    private final PermissionService permissionService;
    private final WorkflowEngine workflowEngine;
    private final OperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;

    public List<TaskTemplateVo> list(LoginUser user) {
        requireTemplateAccess(user);
        return templateMapper.selectList(new LambdaQueryWrapper<TaskTemplate>()
                        .orderByDesc(TaskTemplate::getUpdatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public TaskTemplateVo get(LoginUser user, Integer templateId) {
        requireTemplateAccess(user);
        return toVo(requireTemplate(templateId));
    }

    @Transactional
    public TaskTemplateVo save(LoginUser user, SaveTemplateRequest request) {
        permissionService.requirePermission(user, "task:config");
        if (request.getFlowConfig() == null) {
            throw new BusinessException(400, "流程配置不能为空");
        }
        workflowEngine.validate(request.getFlowConfig());

        TaskTemplate template = new TaskTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setDescription(htmlSanitizer.sanitize(request.getDescription()));
        template.setConfigJson(workflowEngine.toJson(request.getFlowConfig()));
        template.setCreatorId(user.getUserId());
        templateMapper.insert(template);

        operationLogService.log(user, "template:create", "task_template", template.getTemplateId(),
                java.util.Map.of("name", template.getTemplateName()));
        return toVo(template);
    }

    @Transactional
    public TaskTemplateVo saveFromTask(LoginUser user, Integer taskId, SaveTemplateRequest request) {
        permissionService.requirePermission(user, "task:config");
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!StringUtils.hasText(task.getConfigJson())) {
            throw new BusinessException(400, "任务无流程配置，无法保存为模板");
        }

        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        workflowEngine.validate(config);

        TaskTemplate template = new TaskTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setDescription(htmlSanitizer.sanitize(StringUtils.hasText(request.getDescription())
                ? request.getDescription()
                : task.getDescription()));
        template.setConfigJson(task.getConfigJson());
        template.setCreatorId(user.getUserId());
        templateMapper.insert(template);

        operationLogService.log(user, "template:from_task", "task_template", template.getTemplateId(),
                java.util.Map.of("taskId", taskId, "name", template.getTemplateName()));
        return toVo(template);
    }

    @Transactional
    public TaskVo createTaskFromTemplate(LoginUser user, Integer templateId, CreateFromTemplateRequest request) {
        permissionService.requirePermission(user, "task:create");
        TaskTemplate template = requireTemplate(templateId);
        TaskFlowConfig config = workflowEngine.parseJson(template.getConfigJson());
        workflowEngine.validate(config);

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTaskName(StringUtils.hasText(request.getTaskName())
                ? request.getTaskName()
                : template.getTemplateName() + "（副本）");
        createRequest.setDescription(htmlSanitizer.sanitize(StringUtils.hasText(request.getDescription())
                ? request.getDescription()
                : template.getDescription()));
        createRequest.setFlowConfig(config);

        TaskVo task = taskService.createTask(user, createRequest);
        operationLogService.log(user, "template:use", "task_template", templateId,
                java.util.Map.of("taskId", task.getTaskId()));
        return task;
    }

    @Transactional
    public void delete(LoginUser user, Integer templateId) {
        permissionService.requirePermission(user, "task:config");
        TaskTemplate template = requireTemplate(templateId);
        if (!user.getUserId().equals(template.getCreatorId())
                && user.getCurrentGroupId() != null
                && user.getCurrentGroupId() != 1) {
            throw new BusinessException(403, "仅创建者或系统管理员可删除模板");
        }
        templateMapper.deleteById(templateId);
        operationLogService.log(user, "template:delete", "task_template", templateId, null);
    }

    private void requireTemplateAccess(LoginUser user) {
        if (!permissionService.hasPermission(user, "task:create")
                && !permissionService.hasPermission(user, "task:config")) {
            throw new BusinessException(403, "无任务模板访问权限");
        }
    }

    private TaskTemplate requireTemplate(Integer templateId) {
        TaskTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        return template;
    }

    private TaskTemplateVo toVo(TaskTemplate template) {
        TaskFlowConfig config = workflowEngine.parseJson(template.getConfigJson());
        String creatorName = null;
        if (template.getCreatorId() != null) {
            User creator = userMapper.selectById(template.getCreatorId());
            creatorName = creator != null ? creator.getName() : null;
        }
        int nodeCount = config.getNodes() != null ? config.getNodes().size() : 0;
        return TaskTemplateVo.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .flowConfig(config)
                .creatorId(template.getCreatorId())
                .creatorName(creatorName)
                .nodeCount(nodeCount)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
