package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("college")
public class College {

    @TableId(type = IdType.AUTO)
    private Integer collegeId;
    private String collegeName;
    private String collegeCode;
    private Integer status;
    private LocalDateTime createdAt;
}
