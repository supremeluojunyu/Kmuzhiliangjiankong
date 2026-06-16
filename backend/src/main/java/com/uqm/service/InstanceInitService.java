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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InstanceInitService {

    private final TaskInstanceMapper instanceMapper;
    private final NodeRecordMapper nodeRecordMapper;
    private final WorkflowRuntime workflowRuntime;

    @Transactional
    public void initNodeRecords(Integer instanceId, TaskFlowConfig config) {
        List<FlowNode> entryNodes = workflowRuntime.getEntryNodes(config);
        Set<String> entryIds = new HashSet<>();
        for (FlowNode node : entryNodes) {
            entryIds.add(node.getNodeId());
        }

        LocalDateTime now = LocalDateTime.now();
        String firstCurrent = null;
        for (FlowNode node : config.getNodes()) {
            NodeRecord record = new NodeRecord();
            record.setTaskInstanceId(instanceId);
            record.setNodeId(node.getNodeId());
            if (entryIds.contains(node.getNodeId())) {
                record.setStatus("in_progress");
                record.setStartTime(now);
                if (firstCurrent == null) {
                    firstCurrent = node.getNodeId();
                }
            } else {
                record.setStatus("pending");
            }
            nodeRecordMapper.insert(record);
        }

        TaskInstance instance = instanceMapper.selectById(instanceId);
        if (instance != null) {
            instance.setCurrentNodeId(firstCurrent);
            instance.setStatus("in_progress");
            instanceMapper.updateById(instance);
        }
    }
}
