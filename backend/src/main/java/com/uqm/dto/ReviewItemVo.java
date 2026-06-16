package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewItemVo {
    private Integer instanceId;
    private String userName;
    private String collegeName;
    private Double score;
    private String grade;
    private String comment;
    private String nodeId;
    private String nodeName;
}
