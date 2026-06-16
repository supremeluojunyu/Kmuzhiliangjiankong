package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.GroupPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupPermissionMapper extends BaseMapper<GroupPermission> {

    @Delete("DELETE FROM group_permission WHERE group_id = #{groupId}")
    void deleteByGroupId(@Param("groupId") Integer groupId);

    @Insert("""
            <script>
            INSERT INTO group_permission (group_id, permission_id) VALUES
            <foreach collection="permissionIds" item="pid" separator=",">
              (#{groupId}, #{pid})
            </foreach>
            </script>
            """)
    void batchInsert(@Param("groupId") Integer groupId, @Param("permissionIds") List<Integer> permissionIds);

    @Select("""
            SELECT p.permission_id FROM permission p
            JOIN group_permission gp ON gp.permission_id = p.permission_id
            WHERE gp.group_id = #{groupId}
            """)
    List<Integer> listPermissionIdsByGroup(@Param("groupId") Integer groupId);
}
