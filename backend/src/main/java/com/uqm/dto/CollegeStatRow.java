package com.uqm.dto;

import lombok.Data;

@Data
public class CollegeStatRow {
    private Integer collegeId;
    private String collegeName;
    private Long total;
    private Long completed;
}
