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
    /** 当前身份组是否可操作该节点（进行中/草稿且归属本组） */
    private Boolean canOperate;
}
