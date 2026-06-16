package com.uqm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskFlowConfig {
    private Integer taskId;
    private List<FlowNode> nodes;
    private String globalTimeStart;
    private String globalTimeEnd;
}
