package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCollegeRequest {
    @NotBlank(message = "学院名称不能为空")
    private String collegeName;
    private String collegeCode;
    private Integer status;
}
