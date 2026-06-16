package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.MessageTargetGroup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageTargetGroupMapper extends BaseMapper<MessageTargetGroup> {

    @Insert("""
            <script>
            INSERT INTO message_target_group (message_id, group_id) VALUES
            <foreach collection="groupIds" item="gid" separator=",">
              (#{messageId}, #{gid})
            </foreach>
            </script>
            """)
    void batchInsert(@Param("messageId") Integer messageId, @Param("groupIds") List<Integer> groupIds);
}
