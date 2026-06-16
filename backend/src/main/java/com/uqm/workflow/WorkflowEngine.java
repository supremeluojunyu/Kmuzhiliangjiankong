package com.uqm.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.FlowNode;
import com.uqm.dto.TaskFlowConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private static final Set<String> NODE_TYPES = Set.of("submit", "view", "score", "approve");
    private static final Set<String> EXECUTION_MODES = Set.of("sequential", "parallel", "any");

    private final ObjectMapper objectMapper;

    public String toJson(TaskFlowConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "流程配置序列化失败");
        }
    }

    public TaskFlowConfig parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new TaskFlowConfig();
        }
        try {
            return objectMapper.readValue(json, TaskFlowConfig.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "流程配置 JSON 格式错误");
        }
    }

    public void validate(TaskFlowConfig config) {
        if (config == null || config.getNodes() == null || config.getNodes().isEmpty()) {
            throw new BusinessException(400, "至少配置一个流程节点");
        }
        List<FlowNode> nodes = config.getNodes();
        Set<String> nodeIds = new HashSet<>();
        for (FlowNode node : nodes) {
            if (!StringUtils.hasText(node.getNodeId())) {
                throw new BusinessException(400, "节点 node_id 不能为空");
            }
            if (!nodeIds.add(node.getNodeId())) {
                throw new BusinessException(400, "节点 ID 重复：" + node.getNodeId());
            }
            if (!StringUtils.hasText(node.getNodeType()) || !NODE_TYPES.contains(node.getNodeType())) {
                throw new BusinessException(400, "无效节点类型：" + node.getNodeId());
            }
            if (node.getExecuteGroupId() == null) {
                throw new BusinessException(400, "节点执行组不能为空：" + node.getNodeId());
            }
            if (node.getDependsOn() == null) {
                node.setDependsOn(List.of());
            }
            if (StringUtils.hasText(node.getExecutionMode())
                    && !EXECUTION_MODES.contains(node.getExecutionMode())) {
                throw new BusinessException(400, "无效 execution_mode：" + node.getNodeId());
            }
        }
        for (FlowNode node : nodes) {
            for (String dep : node.getDependsOn()) {
                if (!nodeIds.contains(dep)) {
                    throw new BusinessException(400, "节点 " + node.getNodeId() + " 依赖不存在：" + dep);
                }
                if (dep.equals(node.getNodeId())) {
                    throw new BusinessException(400, "节点不能依赖自身：" + node.getNodeId());
                }
            }
        }
        if (hasCycle(nodes)) {
            throw new BusinessException(400, "流程存在循环依赖");
        }
    }

    private boolean hasCycle(List<FlowNode> nodes) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (FlowNode node : nodes) {
            if (dfs(node.getNodeId(), nodes, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfs(String nodeId, List<FlowNode> nodes, Set<String> visiting, Set<String> visited) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (visiting.contains(nodeId)) {
            return true;
        }
        visiting.add(nodeId);
        FlowNode node = nodes.stream().filter(n -> n.getNodeId().equals(nodeId)).findFirst().orElse(null);
        if (node != null && node.getDependsOn() != null) {
            for (String dep : node.getDependsOn()) {
                if (dfs(dep, nodes, visiting, visited)) {
                    return true;
                }
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }
}
