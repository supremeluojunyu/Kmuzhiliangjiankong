package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.TaskInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskInstanceMapper extends BaseMapper<TaskInstance> {

    @Select("""
            SELECT COUNT(*) FROM task_instance
            WHERE assigned_to_user_id = #{userId}
              AND target_group_id = #{groupId}
              AND status IN ('pending', 'in_progress', 'overdue')
            """)
    long countPendingByUserAndGroup(@Param("userId") Integer userId, @Param("groupId") Integer groupId);
}
