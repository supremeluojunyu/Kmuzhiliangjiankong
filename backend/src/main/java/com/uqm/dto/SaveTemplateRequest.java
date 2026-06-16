package com.uqm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveTemplateRequest {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String description;
    @Valid
    private TaskFlowConfig flowConfig;
}
