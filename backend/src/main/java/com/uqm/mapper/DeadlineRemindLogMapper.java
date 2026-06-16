package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.DeadlineRemindLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeadlineRemindLogMapper extends BaseMapper<DeadlineRemindLog> {

    @Select("""
            SELECT COUNT(*) FROM deadline_remind_log
            WHERE instance_id = #{instanceId} AND remind_key = #{remindKey}
            """)
    long countByInstanceAndKey(@Param("instanceId") Integer instanceId, @Param("remindKey") String remindKey);
}
