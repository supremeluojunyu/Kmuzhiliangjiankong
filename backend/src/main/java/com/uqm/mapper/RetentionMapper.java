package com.uqm.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RetentionMapper {

    @Delete("DELETE FROM operation_log WHERE created_at < #{before}")
    int deleteLogsBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE FROM message_read_status WHERE message_id IN (SELECT message_id FROM message WHERE send_time < #{before})")
    int deleteMessageReadBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE FROM message_target_group WHERE message_id IN (SELECT message_id FROM message WHERE send_time < #{before})")
    int deleteMessageTargetsBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE FROM message WHERE send_time < #{before}")
    int deleteMessagesBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE nr FROM node_record nr JOIN task_instance ti ON ti.id = nr.task_instance_id WHERE ti.status = 'completed' AND ti.completed_at < #{before}")
    int deleteNodeRecordsBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE FROM task_instance WHERE status = 'completed' AND completed_at < #{before}")
    int deleteTaskInstancesBefore(@Param("before") LocalDateTime before);
}
