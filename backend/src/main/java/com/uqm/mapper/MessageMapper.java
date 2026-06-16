package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.dto.MessageRow;
import com.uqm.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    String USER_MESSAGE_ACCESS = """
            (
              EXISTS (
                SELECT 1 FROM message_target_user mtu
                WHERE mtu.message_id = m.message_id AND mtu.user_id = #{userId}
              )
              OR EXISTS (
                SELECT 1 FROM message_target_group mtg
                JOIN user_group ug ON ug.group_id = mtg.group_id AND ug.user_id = #{userId}
                WHERE mtg.message_id = m.message_id
              )
            )
            """;

    @Select("""
            SELECT COUNT(DISTINCT m.message_id) FROM message m
            LEFT JOIN message_read_status mrs ON mrs.message_id = m.message_id AND mrs.user_id = #{userId}
            WHERE """ + USER_MESSAGE_ACCESS + """
              AND (mrs.is_read IS NULL OR mrs.is_read = 0)
            """)
    long countUnreadAllGroups(@Param("userId") Integer userId);

    @Select("""
            SELECT COUNT(DISTINCT m.message_id) FROM message m
            LEFT JOIN message_read_status mrs ON mrs.message_id = m.message_id AND mrs.user_id = #{userId}
            WHERE """ + USER_MESSAGE_ACCESS + """
              AND (
                EXISTS (SELECT 1 FROM message_target_group mtg WHERE mtg.message_id = m.message_id AND mtg.group_id = #{groupId})
                OR EXISTS (
                  SELECT 1 FROM message_target_user mtu
                  JOIN user_group ug ON ug.user_id = mtu.user_id AND ug.group_id = #{groupId}
                  WHERE mtu.message_id = m.message_id AND mtu.user_id = #{userId}
                )
              )
              AND (mrs.is_read IS NULL OR mrs.is_read = 0)
            """)
    long countUnreadByGroup(@Param("userId") Integer userId, @Param("groupId") Integer groupId);

    @Select("""
            SELECT DISTINCT m.message_id AS messageId, m.sender_id AS senderId, m.title, m.content,
                   m.message_type AS messageType, m.task_id AS taskId, m.instance_id AS instanceId,
                   m.send_time AS sendTime, u.name AS senderName,
                   IFNULL(mrs.is_read, 0) AS isRead
            FROM message m
            LEFT JOIN `user` u ON u.user_id = m.sender_id
            LEFT JOIN message_read_status mrs ON mrs.message_id = m.message_id AND mrs.user_id = #{userId}
            WHERE """ + USER_MESSAGE_ACCESS + """
            ORDER BY m.send_time DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<MessageRow> listAllForUser(@Param("userId") Integer userId,
                                    @Param("offset") long offset,
                                    @Param("pageSize") long pageSize);

    @Select("""
            SELECT COUNT(DISTINCT m.message_id) FROM message m
            WHERE """ + USER_MESSAGE_ACCESS)
    long countAllForUser(@Param("userId") Integer userId);

    @Select("""
            SELECT g.group_name FROM message_target_group mtg
            JOIN `group` g ON g.group_id = mtg.group_id
            WHERE mtg.message_id = #{messageId}
            """)
    List<String> listTargetGroupNames(@Param("messageId") Integer messageId);

    @Select("""
            SELECT u.name FROM message_target_user mtu
            JOIN `user` u ON u.user_id = mtu.user_id
            WHERE mtu.message_id = #{messageId}
            """)
    List<String> listTargetUserNames(@Param("messageId") Integer messageId);

    @Select("""
            SELECT COUNT(*) FROM message m
            WHERE m.message_id = #{messageId}
              AND """ + USER_MESSAGE_ACCESS)
    long userCanAccess(@Param("userId") Integer userId, @Param("messageId") Integer messageId);

    @Select("""
            SELECT DISTINCT m.message_id AS messageId, m.sender_id AS senderId, m.title, m.content,
                   m.message_type AS messageType, m.task_id AS taskId, m.instance_id AS instanceId,
                   m.send_time AS sendTime, u.name AS senderName,
                   IFNULL(mrs.is_read, 0) AS isRead
            FROM message m
            JOIN message_target_group mtg ON mtg.message_id = m.message_id
            JOIN user_group ug ON ug.group_id = mtg.group_id AND ug.user_id = #{userId}
            LEFT JOIN `user` u ON u.user_id = m.sender_id
            LEFT JOIN message_read_status mrs ON mrs.message_id = m.message_id AND mrs.user_id = #{userId}
            WHERE m.instance_id = #{instanceId} AND m.message_type = 'comment'
            ORDER BY m.send_time ASC
            """)
    List<MessageRow> listCommentsForInstance(@Param("userId") Integer userId,
                                             @Param("instanceId") Integer instanceId);
}
