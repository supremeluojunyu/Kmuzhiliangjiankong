package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.dto.GroupDto;
import com.uqm.entity.UserGroupRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserGroupRelationMapper extends BaseMapper<UserGroupRelation> {

    @Select("""
            SELECT g.group_id AS groupId, g.group_name AS groupName,
                   ug.is_default AS isDefault,
                   (SELECT COUNT(*) FROM task_instance ti
                    WHERE ti.assigned_to_user_id = #{userId}
                      AND ti.target_group_id = g.group_id
                      AND ti.status IN ('pending', 'in_progress', 'overdue')) AS pendingCount
            FROM user_group ug
            JOIN `group` g ON g.group_id = ug.group_id
            WHERE ug.user_id = #{userId}
            ORDER BY ug.sort_order, g.group_id
            """)
    List<GroupDto> listUserGroupsWithPending(@Param("userId") Integer userId);

    @Delete("DELETE FROM user_group WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Integer userId);

    @Delete("DELETE FROM user_group WHERE group_id = #{groupId}")
    void deleteByGroupId(@Param("groupId") Integer groupId);

    @Insert("""
            <script>
            INSERT INTO user_group (user_id, group_id, is_default, sort_order) VALUES
            <foreach collection="groupIds" item="gid" index="idx" separator=",">
              (#{userId}, #{gid},
               <choose><when test="defaultGroupId != null and defaultGroupId == gid">1</when><otherwise>0</otherwise></choose>,
               #{idx})
            </foreach>
            </script>
            """)
    void batchInsert(@Param("userId") Integer userId,
                     @Param("groupIds") List<Integer> groupIds,
                     @Param("defaultGroupId") Integer defaultGroupId);

    @Select("SELECT COUNT(*) FROM user_group WHERE group_id = #{groupId}")
    long countMembers(@Param("groupId") Integer groupId);

    @Select("SELECT COUNT(*) FROM user_group WHERE user_id = #{userId} AND group_id = #{groupId}")
    long countUserInGroup(@Param("userId") Integer userId, @Param("groupId") Integer groupId);
}
