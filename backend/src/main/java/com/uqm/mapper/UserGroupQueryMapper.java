package com.uqm.mapper;

import com.uqm.dto.UserPoolItem;
import com.uqm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserGroupQueryMapper {

    @Select("""
            <script>
            SELECT u.user_id AS userId, u.college_id AS collegeId, u.name, u.account
            FROM user u
            JOIN user_group ug ON ug.user_id = u.user_id
            WHERE ug.group_id = #{groupId} AND u.status = 1
            <if test="collegeIds != null and collegeIds.size() > 0">
              AND u.college_id IN
              <foreach collection="collegeIds" item="cid" open="(" separator="," close=")">
                #{cid}
              </foreach>
            </if>
            </script>
            """)
    List<UserPoolItem> listUsersInGroup(@Param("groupId") Integer groupId,
                                         @Param("collegeIds") List<Integer> collegeIds);
}
