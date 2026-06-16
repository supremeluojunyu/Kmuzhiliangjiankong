package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollegeScoreVo {
    private Integer collegeId;
    private String collegeName;
    private Double average;
    private Double max;
    private Double min;
    private long count;
}
