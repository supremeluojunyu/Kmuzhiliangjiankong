package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class NodeRecordVo {
    private Integer id;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer executeGroupId;
    private String executeGroupName;
    private Map<String, Object> config;
    private String status;
    private Map<String, Object> submitData;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
