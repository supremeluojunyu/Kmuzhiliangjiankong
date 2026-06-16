package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer groupId;
    private String action;
    private String targetType;
    private Integer targetId;
    @TableField("detail")
    private String detailJson;
    private String ip;
    private LocalDateTime createdAt;
}
