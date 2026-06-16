package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
import com.uqm.dto.FlowNode;
import com.uqm.dto.MyTaskVo;
import com.uqm.dto.NodeRecordVo;
import com.uqm.dto.SubmitNodeRequest;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.NodeRecord;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskInstance;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.NodeRecordMapper;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.ScoreConfigUtil;
import com.uqm.workflow.WorkflowEngine;
import com.uqm.workflow.WorkflowRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final TaskInstanceMapper instanceMapper;
    private final GroupMapper groupMapper;
    private final TaskDefinitionMapper taskMapper;
    private final NodeRecordMapper nodeRecordMapper;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRuntime workflowRuntime;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;

    public PageResult<MyTaskVo> listMyTasks(LoginUser user, long page, long pageSize, String filter) {
        if (user.getCurrentGroupId() == null) {
            throw new BusinessException(403, "请先选择身份组");
        }

        Set<Integer> instanceIds = new LinkedHashSet<>();

        LambdaQueryWrapper<TaskInstance> statusWrapper = new LambdaQueryWrapper<TaskInstance>()
                .orderByDesc(TaskInstance::getCreatedAt);
        applyInstanceStatusFilter(statusWrapper, filter);

        if (canViewAllNodes(user)) {
            instanceMapper.selectList(statusWrapper).forEach(i -> instanceIds.add(i.getId()));
        } else {
            for (TaskInstance instance : instanceMapper.selectList(statusWrapper)) {
                if (isInstanceVisibleToUser(user, instance)) {
                    instanceIds.add(instance.getId());
                }
            }
        }

        if (instanceIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }

        LambdaQueryWrapper<TaskInstance> wrapper = new LambdaQueryWrapper<TaskInstance>()
                .in(TaskInstance::getId, instanceIds)
                .orderByDesc(TaskInstance::getCreatedAt);

        long total = instanceMapper.selectCount(wrapper);
        long offset = (page - 1) * pageSize;
        wrapper.last("LIMIT " + offset + "," + pageSize);
        List<TaskInstance> instances = instanceMapper.selectList(wrapper);

        List<MyTaskVo> list = instances.stream()
                .map(i -> toMyTaskVo(i, user))
                .toList();

        return new PageResult<>(list, total, page, pageSize);
    }

    public MyTaskVo getInstanceDetail(LoginUser user, Integer instanceId) {
        TaskInstance instance = requireAccessibleInstance(user, instanceId);
        return toMyTaskVoDetail(instance, user);
    }

    @Transactional
    public MyTaskVo submitNode(LoginUser user, Integer instanceId, String nodeId, SubmitNodeRequest request) {
        TaskInstance instance = requireAccessibleInstance(user, instanceId);
        if ("completed".equals(instance.getStatus()) || "closed".equals(instance.getStatus())) {
            throw new BusinessException(400, "任务实例已结束");
        }

        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        requireTaskRunnable(task);
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        FlowNode nodeDef = workflowRuntime.getNode(config, nodeId);
        if (nodeDef == null) {
            throw new BusinessException(404, "节点不存在");
        }
        if (!nodeDef.getExecuteGroupId().equals(user.getCurrentGroupId())) {
            throw new BusinessException(403, "当前身份组无权执行该节点");
        }

        NodeRecord record = nodeRecordMapper.selectOne(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instanceId)
                .eq(NodeRecord::getNodeId, nodeId));
        if (record == null) {
            throw new BusinessException(404, "节点记录不存在");
        }
        if ("completed".equals(record.getStatus())) {
            throw new BusinessException(400, "该节点已完成");
        }
        if ("pending".equals(record.getStatus())) {
            throw new BusinessException(400, "该节点尚未激活");
        }

        boolean isDraft = Boolean.TRUE.equals(request.getDraft());
        try {
            record.setSubmitData(objectMapper.writeValueAsString(request.getSubmitData()));
        } catch (Exception e) {
            throw new BusinessException(400, "提交数据格式错误");
        }

        if (isDraft) {
            record.setStatus("draft");
            nodeRecordMapper.updateById(record);
            return toMyTaskVoDetail(instance, user);
        }

        validateSubmit(nodeDef, request.getSubmitData());

        record.setStatus("completed");
        record.setEndTime(LocalDateTime.now());
        nodeRecordMapper.updateById(record);

        advanceWorkflow(instance, config);
        return toMyTaskVoDetail(instanceMapper.selectById(instanceId), user);
    }

    private void validateSubmit(FlowNode nodeDef, Map<String, Object> data) {
        if (data == null) {
            data = Map.of();
        }
        switch (nodeDef.getNodeType()) {
            case "submit" -> {
                // 材料提交允许空文件列表（后续可接文件上传）
            }
            case "score" -> ScoreConfigUtil.validateScoreSubmit(nodeDef, data);
            case "approve" -> {
                if (!data.containsKey("result")) {
                    throw new BusinessException(400, "请选择审核结果");
                }
            }
            case "view" -> { /* 查看节点自动完成 */ }
            default -> throw new BusinessException(400, "未知节点类型");
        }
    }

    private void advanceWorkflow(TaskInstance instance, TaskFlowConfig config) {
        List<NodeRecord> allRecords = nodeRecordMapper.selectList(
                new LambdaQueryWrapper<NodeRecord>().eq(NodeRecord::getTaskInstanceId, instance.getId()));

        Set<String> completed = allRecords.stream()
                .filter(r -> "completed".equals(r.getStatus()))
                .map(NodeRecord::getNodeId)
                .collect(Collectors.toSet());

        Set<String> inProgress = allRecords.stream()
                .filter(r -> "in_progress".equals(r.getStatus()) || "draft".equals(r.getStatus()))
                .map(NodeRecord::getNodeId)
                .collect(Collectors.toSet());

        List<FlowNode> activatable = workflowRuntime.getActivatableNodes(config, completed);
        LocalDateTime now = LocalDateTime.now();
        for (FlowNode node : activatable) {
            NodeRecord nr = allRecords.stream()
                    .filter(r -> r.getNodeId().equals(node.getNodeId()))
                    .findFirst().orElse(null);
            if (nr != null && "pending".equals(nr.getStatus())) {
                nr.setStatus("in_progress");
                nr.setStartTime(now);
                nodeRecordMapper.updateById(nr);
                inProgress.add(node.getNodeId());
            }
        }

        if (workflowRuntime.isAllCompleted(config, completed)) {
            instance.setStatus("completed");
            instance.setCompletedAt(now);
            instance.setCurrentNodeId(null);
        } else {
            instance.setStatus("in_progress");
            String current = workflowRuntime.findCurrentNodeId(config, inProgress);
            if (StringUtils.hasText(current)) {
                instance.setCurrentNodeId(current);
            }
        }
        instanceMapper.updateById(instance);
    }

    private void applyInstanceStatusFilter(LambdaQueryWrapper<TaskInstance> wrapper, String filter) {
        if ("pending".equals(filter) || "in_progress".equals(filter)) {
            wrapper.in(TaskInstance::getStatus, List.of("pending", "in_progress", "overdue"));
        } else if ("completed".equals(filter)) {
            wrapper.eq(TaskInstance::getStatus, "completed");
        } else if ("overdue".equals(filter)) {
            wrapper.eq(TaskInstance::getStatus, "overdue");
        }
    }

    private boolean canViewAllNodes(LoginUser user) {
        return permissionService.hasPermission(user, "stat:view_all")
                || permissionService.hasPermission(user, "system:config");
    }

    private boolean isInstanceVisibleToUser(LoginUser user, TaskInstance instance) {
        if (canViewAllNodes(user)) {
            return true;
        }
        Integer groupId = user.getCurrentGroupId();
        TaskFlowConfig config = loadFlowConfig(instance.getTaskDefinitionId());
        if (config == null || !groupInFlow(config, groupId)) {
            return false;
        }
        if (hasActiveNodeForGroup(instance, groupId)) {
            if (instance.getAssignedToUserId().equals(user.getUserId())) {
                return true;
            }
            FlowNode active = findActiveNodeDef(config, loadNodeRecords(instance.getId()), groupId);
            if (active != null && isDownstreamNode(config, active)) {
                return true;
            }
        }
        if (instance.getAssignedToUserId().equals(user.getUserId())
                && instance.getTargetGroupId().equals(groupId)) {
            return hasGroupParticipation(instance, config, groupId);
        }
        return false;
    }

    private boolean isDownstreamNode(TaskFlowConfig config, FlowNode node) {
        return workflowRuntime.getEntryNodes(config).stream()
                .noneMatch(entry -> entry.getNodeId().equals(node.getNodeId()));
    }

    private boolean groupInFlow(TaskFlowConfig config, Integer groupId) {
        return config.getNodes().stream().anyMatch(n -> groupId.equals(n.getExecuteGroupId()));
    }

    private boolean hasGroupParticipation(TaskInstance instance, TaskFlowConfig config, Integer groupId) {
        List<NodeRecord> records = loadNodeRecords(instance.getId());
        for (NodeRecord record : records) {
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node == null || !groupId.equals(node.getExecuteGroupId())) {
                continue;
            }
            if (!"pending".equals(record.getStatus())) {
                return true;
            }
        }
        return workflowRuntime.getEntryNodes(config).stream()
                .anyMatch(n -> groupId.equals(n.getExecuteGroupId()));
    }

    private TaskFlowConfig loadFlowConfig(Integer taskDefinitionId) {
        TaskDefinition task = taskMapper.selectById(taskDefinitionId);
        if (task == null) {
            return null;
        }
        return workflowEngine.parseJson(task.getConfigJson());
    }

    private List<NodeRecord> loadNodeRecords(Integer instanceId) {
        return nodeRecordMapper.selectList(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instanceId));
    }

    private TaskInstance requireAccessibleInstance(LoginUser user, Integer instanceId) {
        TaskInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(404, "任务实例不存在");
        }
        if (user.getCurrentGroupId() == null) {
            throw new BusinessException(403, "请先选择身份组");
        }
        if (canViewAllNodes(user) || isInstanceVisibleToUser(user, instance)) {
            return instance;
        }
        throw new BusinessException(403, "无权访问该任务实例");
    }

    private boolean hasActiveNodeForGroup(TaskInstance instance, Integer groupId) {
        TaskFlowConfig config = loadFlowConfig(instance.getTaskDefinitionId());
        if (config == null) {
            return false;
        }
        List<NodeRecord> records = loadNodeRecords(instance.getId());
        for (NodeRecord record : records) {
            if (!List.of("in_progress", "draft").contains(record.getStatus())) {
                continue;
            }
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node != null && groupId.equals(node.getExecuteGroupId())) {
                return true;
            }
        }
        return false;
    }

    private FlowNode findActiveNodeDef(TaskFlowConfig config, List<NodeRecord> records, Integer groupId) {
        for (NodeRecord record : records) {
            if (!List.of("in_progress", "draft").contains(record.getStatus())) {
                continue;
            }
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node != null && groupId.equals(node.getExecuteGroupId())) {
                return node;
            }
        }
        return null;
    }

    private Set<String> upstreamNodeIds(TaskFlowConfig config, String nodeId) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        FlowNode node = workflowRuntime.getNode(config, nodeId);
        if (node != null && node.getDependsOn() != null) {
            queue.addAll(node.getDependsOn());
        }
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!result.add(id)) {
                continue;
            }
            FlowNode upstream = workflowRuntime.getNode(config, id);
            if (upstream != null && upstream.getDependsOn() != null) {
                queue.addAll(upstream.getDependsOn());
            }
        }
        return result;
    }

    private List<NodeRecordVo> collectReferenceMaterials(
            TaskFlowConfig config, List<NodeRecord> records, FlowNode activeNode) {
        if (activeNode == null || !List.of("score", "approve", "view").contains(activeNode.getNodeType())) {
            return List.of();
        }
        Set<String> upstream = upstreamNodeIds(config, activeNode.getNodeId());
        List<NodeRecordVo> materials = new ArrayList<>();
        for (NodeRecord record : records) {
            if (!upstream.contains(record.getNodeId()) || !"completed".equals(record.getStatus())) {
                continue;
            }
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node == null || !List.of("submit", "view").contains(node.getNodeType())) {
                continue;
            }
            materials.add(toNodeRecordVo(record, config));
        }
        return materials;
    }

    private void applyCurrentNodeForUser(
            MyTaskVo vo, TaskInstance instance, TaskFlowConfig config, LoginUser user, List<NodeRecord> records) {
        if (canViewAllNodes(user)) {
            FlowNode currentNode = instance.getCurrentNodeId() != null
                    ? workflowRuntime.getNode(config, instance.getCurrentNodeId()) : null;
            if (currentNode != null) {
                vo.setCurrentNodeId(currentNode.getNodeId());
                vo.setCurrentNodeName(currentNode.getNodeName());
                vo.setCurrentNodeType(currentNode.getNodeType());
            }
            return;
        }
        Integer groupId = user.getCurrentGroupId();
        FlowNode active = findActiveNodeDef(config, records, groupId);
        if (active != null) {
            vo.setCurrentNodeId(active.getNodeId());
            vo.setCurrentNodeName(active.getNodeName());
            vo.setCurrentNodeType(active.getNodeType());
            return;
        }
        for (NodeRecord record : records) {
            FlowNode node = workflowRuntime.getNode(config, record.getNodeId());
            if (node != null && groupId.equals(node.getExecuteGroupId()) && "completed".equals(record.getStatus())) {
                vo.setCurrentNodeId(node.getNodeId());
                vo.setCurrentNodeName(node.getNodeName());
                vo.setCurrentNodeType(node.getNodeType());
            }
        }
    }

    private MyTaskVo toMyTaskVo(TaskInstance instance, LoginUser user) {
        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        TaskFlowConfig config = task != null ? workflowEngine.parseJson(task.getConfigJson()) : null;
        List<NodeRecord> records = loadNodeRecords(instance.getId());

        MyTaskVo vo = MyTaskVo.builder()
                .instanceId(instance.getId())
                .taskId(instance.getTaskDefinitionId())
                .taskName(task != null ? task.getTaskName() : null)
                .status(instance.getStatus())
                .createdAt(instance.getCreatedAt())
                .completedAt(instance.getCompletedAt())
                .fullView(canViewAllNodes(user))
                .build();
        if (config != null) {
            applyCurrentNodeForUser(vo, instance, config, user, records);
        }
        return vo;
    }

    private MyTaskVo toMyTaskVoDetail(TaskInstance instance, LoginUser user) {
        MyTaskVo vo = toMyTaskVo(instance, user);
        TaskFlowConfig config = loadFlowConfig(instance.getTaskDefinitionId());
        List<NodeRecord> records = loadNodeRecords(instance.getId());
        if (config == null) {
            return vo;
        }

        List<NodeRecordVo> allRecords = records.stream()
                .map(r -> toNodeRecordVo(r, config))
                .toList();
        if (canViewAllNodes(user)) {
            vo.setNodeRecords(allRecords);
            return vo;
        }

        Integer groupId = user.getCurrentGroupId();
        vo.setNodeRecords(allRecords.stream()
                .filter(r -> groupId.equals(r.getExecuteGroupId()))
                .toList());
        FlowNode activeNode = findActiveNodeDef(config, records, groupId);
        vo.setReferenceMaterials(collectReferenceMaterials(config, records, activeNode));
        return vo;
    }

    private NodeRecordVo toNodeRecordVo(NodeRecord record, TaskFlowConfig config) {
        FlowNode node = config != null ? workflowRuntime.getNode(config, record.getNodeId()) : null;
        Map<String, Object> submitData = null;
        if (StringUtils.hasText(record.getSubmitData())) {
            try {
                submitData = objectMapper.readValue(record.getSubmitData(), new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }
        return NodeRecordVo.builder()
                .id(record.getId())
                .nodeId(record.getNodeId())
                .nodeName(node != null ? node.getNodeName() : record.getNodeId())
                .nodeType(node != null ? node.getNodeType() : null)
                .executeGroupId(node != null ? node.getExecuteGroupId() : null)
                .executeGroupName(resolveGroupName(node != null ? node.getExecuteGroupId() : null))
                .config(node != null ? node.getConfig() : null)
                .status(record.getStatus())
                .submitData(submitData)
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .build();
    }

    private String resolveGroupName(Integer groupId) {
        if (groupId == null) {
            return null;
        }
        UserGroupEntity group = groupMapper.selectById(groupId);
        return group != null ? group.getGroupName() : null;
    }

    private void requireTaskRunnable(TaskDefinition task) {
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if ("paused".equals(task.getStatus())) {
            throw new BusinessException(400, "任务已暂停，暂无法提交");
        }
        if ("closed".equals(task.getStatus())) {
            throw new BusinessException(400, "任务已停止");
        }
    }
}
