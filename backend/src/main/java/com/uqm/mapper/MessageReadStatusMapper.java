package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.MessageReadStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageReadStatusMapper extends BaseMapper<MessageReadStatus> {

    @Insert("""
            INSERT INTO message_read_status (message_id, user_id, is_read)
            VALUES (#{messageId}, #{userId}, 1)
            ON DUPLICATE KEY UPDATE is_read = 1
            """)
    void markRead(@Param("messageId") Integer messageId, @Param("userId") Integer userId);

    @Insert("""
            <script>
            INSERT INTO message_read_status (message_id, user_id, is_read) VALUES
            <foreach collection="messageIds" item="mid" separator=",">
              (#{mid}, #{userId}, 1)
            </foreach>
            ON DUPLICATE KEY UPDATE is_read = 1
            </script>
            """)
    void markReadBatch(@Param("messageIds") List<Integer> messageIds, @Param("userId") Integer userId);
}
