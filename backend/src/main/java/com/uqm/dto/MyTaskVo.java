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
    /** 评分/审核时可查看的上游材料（提交组节点），不含其它执行组节点详情 */
    private List<NodeRecordVo> referenceMaterials;
    /** 管理员全览：可见全部流程节点 */
    private Boolean fullView;
}
