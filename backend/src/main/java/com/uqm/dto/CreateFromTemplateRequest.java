package com.uqm.dto;

import lombok.Data;

@Data
public class CreateFromTemplateRequest {
    private String taskName;
    private String description;
}
