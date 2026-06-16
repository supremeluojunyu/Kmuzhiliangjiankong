package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
            SELECT p.permission_code FROM permission p
            JOIN group_permission gp ON gp.permission_id = p.permission_id
            WHERE gp.group_id = #{groupId}
            """)
    List<String> listCodesByGroupId(@Param("groupId") Integer groupId);
}
