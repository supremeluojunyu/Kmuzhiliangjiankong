package com.uqm.workflow;

import com.uqm.dto.FlowNode;
import com.uqm.dto.TaskFlowConfig;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkflowRuntime {

    public List<FlowNode> getEntryNodes(TaskFlowConfig config) {
        return config.getNodes().stream()
                .filter(n -> n.getDependsOn() == null || n.getDependsOn().isEmpty())
                .toList();
    }

    public FlowNode getNode(TaskFlowConfig config, String nodeId) {
        return config.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public List<FlowNode> getActivatableNodes(TaskFlowConfig config, Set<String> completedNodeIds) {
        return config.getNodes().stream()
                .filter(n -> {
                    if (completedNodeIds.contains(n.getNodeId())) {
                        return false;
                    }
                    List<String> deps = n.getDependsOn();
                    if (deps == null || deps.isEmpty()) {
                        return false;
                    }
                    return completedNodeIds.containsAll(deps);
                })
                .toList();
    }

    public boolean isAllCompleted(TaskFlowConfig config, Set<String> completedNodeIds) {
        Set<String> allIds = config.getNodes().stream()
                .map(FlowNode::getNodeId)
                .collect(Collectors.toSet());
        return completedNodeIds.containsAll(allIds);
    }

    public String findCurrentNodeId(TaskFlowConfig config, Set<String> inProgressNodeIds) {
        if (!inProgressNodeIds.isEmpty()) {
            return inProgressNodeIds.iterator().next();
        }
        for (FlowNode node : config.getNodes()) {
            if (node.getDependsOn() == null || node.getDependsOn().isEmpty()) {
                return node.getNodeId();
            }
        }
        return null;
    }

    public Set<String> downstreamNodeIds(TaskFlowConfig config, String completedNodeId) {
        Set<String> result = new HashSet<>();
        for (FlowNode node : config.getNodes()) {
            if (node.getDependsOn() != null && node.getDependsOn().contains(completedNodeId)) {
                result.add(node.getNodeId());
            }
        }
        return result;
    }
}
