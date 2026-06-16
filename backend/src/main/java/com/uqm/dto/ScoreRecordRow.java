package com.uqm.dto;

import lombok.Data;

@Data
public class ScoreRecordRow {
    private Integer recordId;
    private String nodeId;
    private String submitData;
    private Integer instanceId;
    private String userName;
    private String collegeName;
    private Integer collegeId;
}
