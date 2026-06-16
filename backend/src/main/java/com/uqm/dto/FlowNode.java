package com.uqm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowNode {
    private String nodeId;
    private String nodeType;
    private String nodeName;
    private Integer executeGroupId;
    private List<String> dependsOn;
    private String executionMode;
    private Integer timeLimitHours;
    private Map<String, Object> config;
}
