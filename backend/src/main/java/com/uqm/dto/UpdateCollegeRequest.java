package com.uqm.dto;

import lombok.Data;

@Data
public class UpdateCollegeRequest {
    private String collegeName;
    private String collegeCode;
    private Integer status;
}
