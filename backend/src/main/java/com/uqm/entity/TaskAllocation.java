package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_allocation")
public class TaskAllocation {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer taskId;
    private String allocationType;
    @TableField("params_json")
    private String paramsJson;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
