package com.uqm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostInstanceCommentRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
