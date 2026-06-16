package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`group`")
public class UserGroupEntity {

    @TableId(type = IdType.AUTO)
    private Integer groupId;
    private String groupName;
    private Integer parentGroupId;
    private String description;
    private LocalDateTime createdAt;
}
