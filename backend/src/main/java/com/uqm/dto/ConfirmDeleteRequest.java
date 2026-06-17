package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ConfirmDeleteRequest {
    @NotEmpty(message = "请选择要删除的项")
    private List<Integer> ids;
    @NotBlank(message = "请输入确认语")
    private String confirmPhrase;
}
