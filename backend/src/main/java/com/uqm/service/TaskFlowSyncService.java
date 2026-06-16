package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.dto.FlowNode;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.NodeRecord;
import com.uqm.entity.TaskInstance;
import com.uqm.mapper.NodeRecordMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.workflow.WorkflowRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskFlowSyncService {

    private final TaskInstanceMapper instanceMapper;
    private final NodeRecordMapper nodeRecordMapper;
    private final WorkflowRuntime workflowRuntime;

    @Transactional
    public void syncActiveInstances(Integer taskId, TaskFlowConfig config) {
        List<TaskInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId)
                .in(TaskInstance::getStatus, List.of("pending", "in_progress", "overdue")));

        Set<String> configNodeIds = config.getNodes().stream()
                .map(FlowNode::getNodeId)
                .collect(Collectors.toSet());

        for (TaskInstance instance : instances) {
            syncInstance(instance, config, configNodeIds);
        }
    }

    private void syncInstance(TaskInstance instance, TaskFlowConfig config, Set<String> configNodeIds) {
        List<NodeRecord> records = nodeRecordMapper.selectList(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instance.getId()));

        Set<String> existingIds = records.stream().map(NodeRecord::getNodeId).collect(Collectors.toSet());

        for (FlowNode node : config.getNodes()) {
            if (!existingIds.contains(node.getNodeId())) {
                NodeRecord record = new NodeRecord();
                record.setTaskInstanceId(instance.getId());
                record.setNodeId(node.getNodeId());
                record.setStatus("pending");
                nodeRecordMapper.insert(record);
            }
        }

        for (NodeRecord record : records) {
            if (!configNodeIds.contains(record.getNodeId()) && "pending".equals(record.getStatus())) {
                nodeRecordMapper.deleteById(record.getId());
            }
        }

        List<NodeRecord> updated = nodeRecordMapper.selectList(new LambdaQueryWrapper<NodeRecord>()
                .eq(NodeRecord::getTaskInstanceId, instance.getId()));

        Set<String> completed = updated.stream()
                .filter(r -> "completed".equals(r.getStatus()))
                .map(NodeRecord::getNodeId)
                .collect(Collectors.toSet());

        Set<String> inProgress = updated.stream()
                .filter(r -> "in_progress".equals(r.getStatus()) || "draft".equals(r.getStatus()))
                .map(NodeRecord::getNodeId)
                .collect(Collectors.toSet());

        if (inProgress.isEmpty()) {
            List<FlowNode> activatable = workflowRuntime.getActivatableNodes(config, completed);
            for (FlowNode node : activatable) {
                updated.stream()
                        .filter(r -> r.getNodeId().equals(node.getNodeId()) && "pending".equals(r.getStatus()))
                        .findFirst()
                        .ifPresent(r -> {
                            r.setStatus("in_progress");
                            if (r.getStartTime() == null) {
                                r.setStartTime(java.time.LocalDateTime.now());
                            }
                            nodeRecordMapper.updateById(r);
                            inProgress.add(node.getNodeId());
                        });
            }
            if (inProgress.isEmpty()) {
                List<FlowNode> entries = workflowRuntime.getEntryNodes(config);
                for (FlowNode entry : entries) {
                    if (!completed.contains(entry.getNodeId())) {
                        updated.stream()
                                .filter(r -> r.getNodeId().equals(entry.getNodeId()) && "pending".equals(r.getStatus()))
                                .findFirst()
                                .ifPresent(r -> {
                                    r.setStatus("in_progress");
                                    r.setStartTime(java.time.LocalDateTime.now());
                                    nodeRecordMapper.updateById(r);
                                    inProgress.add(entry.getNodeId());
                                });
                        if (!inProgress.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        String current = workflowRuntime.findCurrentNodeId(config, inProgress);
        instance.setCurrentNodeId(current);
        if (!"completed".equals(instance.getStatus()) && !"closed".equals(instance.getStatus())) {
            instance.setStatus(inProgress.isEmpty() && completed.containsAll(configNodeIds)
                    ? "completed" : "in_progress");
        }
        instanceMapper.updateById(instance);
    }

    @Transactional
    public void closeActiveInstances(Integer taskId) {
        List<TaskInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId)
                .in(TaskInstance::getStatus, List.of("pending", "in_progress", "overdue")));

        for (TaskInstance instance : instances) {
            instance.setStatus("closed");
            instance.setCurrentNodeId(null);
            instanceMapper.updateById(instance);
        }
    }

    public boolean hasActiveInstances(Integer taskId) {
        return instanceMapper.selectCount(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId)
                .in(TaskInstance::getStatus, List.of("pending", "in_progress", "overdue"))) > 0;
    }
}
