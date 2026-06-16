package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_group")
public class UserGroupRelation {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer groupId;
    private Integer isDefault;
    private Integer sortOrder;
}
