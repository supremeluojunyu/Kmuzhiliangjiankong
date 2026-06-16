package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.MessageTargetUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageTargetUserMapper extends BaseMapper<MessageTargetUser> {

    @Insert("""
            <script>
            INSERT INTO message_target_user (message_id, user_id) VALUES
            <foreach collection="userIds" item="uid" separator=",">
              (#{messageId}, #{uid})
            </foreach>
            </script>
            """)
    void batchInsert(@Param("messageId") Integer messageId, @Param("userIds") List<Integer> userIds);
}
