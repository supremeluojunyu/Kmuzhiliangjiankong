package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("group_permission")
public class GroupPermission {
    private Integer groupId;
    private Integer permissionId;
}
