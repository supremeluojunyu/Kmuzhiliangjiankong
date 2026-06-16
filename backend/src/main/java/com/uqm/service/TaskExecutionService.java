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
import java.util.HashSet;
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

    public PageResult<MyTaskVo> listMyTasks(LoginUser user, long page, long pageSize, String filter) {
        if (user.getCurrentGroupId() == null) {
            throw new BusinessException(403, "请先选择身份组");
        }

        LambdaQueryWrapper<TaskInstance> wrapper = new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getAssignedToUserId, user.getUserId())
                .eq(TaskInstance::getTargetGroupId, user.getCurrentGroupId())
                .orderByDesc(TaskInstance::getCreatedAt);

        if ("pending".equals(filter) || "in_progress".equals(filter)) {
            wrapper.in(TaskInstance::getStatus, List.of("pending", "in_progress", "overdue"));
        } else if ("completed".equals(filter)) {
            wrapper.eq(TaskInstance::getStatus, "completed");
        } else if ("overdue".equals(filter)) {
            wrapper.eq(TaskInstance::getStatus, "overdue");
        }

        long total = instanceMapper.selectCount(wrapper);
        long offset = (page - 1) * pageSize;
        wrapper.last("LIMIT " + offset + "," + pageSize);
        List<TaskInstance> instances = instanceMapper.selectList(wrapper);

        List<MyTaskVo> list = instances.stream()
                .map(this::toMyTaskVo)
                .toList();

        return new PageResult<>(list, total, page, pageSize);
    }

    public MyTaskVo getInstanceDetail(LoginUser user, Integer instanceId) {
        TaskInstance instance = requireAccessibleInstance(user, instanceId);
        return toMyTaskVoDetail(instance);
    }

    @Transactional
    public MyTaskVo submitNode(LoginUser user, Integer instanceId, String nodeId, SubmitNodeRequest request) {
        TaskInstance instance = requireAccessibleInstance(user, instanceId);
        if ("completed".equals(instance.getStatus()) || "closed".equals(instance.getStatus())) {
            throw new BusinessException(400, "任务实例已结束");
        }

        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
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
            return toMyTaskVoDetail(instance);
        }

        validateSubmit(nodeDef, request.getSubmitData());

        record.setStatus("completed");
        record.setEndTime(LocalDateTime.now());
        nodeRecordMapper.updateById(record);

        advanceWorkflow(instance, config);
        return toMyTaskVoDetail(instanceMapper.selectById(instanceId));
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

    private TaskInstance requireAccessibleInstance(LoginUser user, Integer instanceId) {
        TaskInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(404, "任务实例不存在");
        }
        if (!instance.getAssignedToUserId().equals(user.getUserId())) {
            throw new BusinessException(403, "无权访问该任务实例");
        }
        if (user.getCurrentGroupId() != null
                && !instance.getTargetGroupId().equals(user.getCurrentGroupId())) {
            throw new BusinessException(403, "请切换到正确的身份组");
        }
        return instance;
    }

    private MyTaskVo toMyTaskVo(TaskInstance instance) {
        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        TaskFlowConfig config = task != null ? workflowEngine.parseJson(task.getConfigJson()) : null;
        FlowNode currentNode = config != null && instance.getCurrentNodeId() != null
                ? workflowRuntime.getNode(config, instance.getCurrentNodeId()) : null;

        return MyTaskVo.builder()
                .instanceId(instance.getId())
                .taskId(instance.getTaskDefinitionId())
                .taskName(task != null ? task.getTaskName() : null)
                .status(instance.getStatus())
                .currentNodeId(instance.getCurrentNodeId())
                .currentNodeName(currentNode != null ? currentNode.getNodeName() : null)
                .currentNodeType(currentNode != null ? currentNode.getNodeType() : null)
                .createdAt(instance.getCreatedAt())
                .completedAt(instance.getCompletedAt())
                .build();
    }

    private MyTaskVo toMyTaskVoDetail(TaskInstance instance) {
        MyTaskVo vo = toMyTaskVo(instance);
        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        TaskFlowConfig config = task != null ? workflowEngine.parseJson(task.getConfigJson()) : null;

        List<NodeRecord> records = nodeRecordMapper.selectList(
                new LambdaQueryWrapper<NodeRecord>().eq(NodeRecord::getTaskInstanceId, instance.getId()));

        vo.setNodeRecords(records.stream().map(r -> toNodeRecordVo(r, config)).toList());
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
}
