package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollegeManageVo {
    private Integer collegeId;
    private String collegeName;
    private String collegeCode;
    private Integer status;
    private LocalDateTime createdAt;
    private long userCount;
    private boolean deletable;
}
