package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MyTaskVo {
    private Integer instanceId;
    private Integer taskId;
    private String taskName;
    private String status;
    private String currentNodeId;
    private String currentNodeName;
    private String currentNodeType;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<NodeRecordVo> nodeRecords;
}
