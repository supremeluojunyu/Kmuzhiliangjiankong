package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_record")
public class NodeRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer taskInstanceId;
    private String nodeId;
    private String status;
    @TableField("submit_data")
    private String submitData;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
