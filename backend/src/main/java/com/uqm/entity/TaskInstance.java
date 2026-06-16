package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_instance")
public class TaskInstance {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer taskDefinitionId;
    private Integer targetGroupId;
    private Integer assignedToUserId;
    private Integer collegeId;
    private String status;
    private String currentNodeId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
