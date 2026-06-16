package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollegeProgressVo {
    private Integer collegeId;
    private String collegeName;
    private long total;
    private long completed;
    private double rate;
}
