package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_definition")
public class TaskDefinition {

    @TableId(type = IdType.AUTO)
    private Integer taskId;
    private String taskName;
    private String description;
    @TableField("config_json")
    private String configJson;
    private String status;
    private Integer creatorId;
    private LocalDateTime createdAt;
}
