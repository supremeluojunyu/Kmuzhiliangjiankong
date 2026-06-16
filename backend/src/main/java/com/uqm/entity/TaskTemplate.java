package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_template")
public class TaskTemplate {
    @TableId(type = IdType.AUTO)
    private Integer templateId;
    private String templateName;
    private String description;
    private String configJson;
    private Integer creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
