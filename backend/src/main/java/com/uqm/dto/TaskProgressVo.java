package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskProgressVo {
    private Integer taskId;
    private String taskName;
    private long totalInstances;
    private long completedInstances;
    private double completionRate;
    private List<NodeProgressVo> nodeProgress;
    private List<CollegeProgressVo> collegeProgress;
    /** all=全校, college=本院 */
    private String scope;
    private String scopeCollegeName;
}
