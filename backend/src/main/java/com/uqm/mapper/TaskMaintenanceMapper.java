package com.uqm.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMaintenanceMapper {

    @Delete("DELETE FROM task_attachment WHERE task_id = #{taskId}")
    void deleteAttachmentsByTaskId(@Param("taskId") Integer taskId);

    @Update("UPDATE message SET task_id = NULL WHERE task_id = #{taskId}")
    void clearMessageTaskReference(@Param("taskId") Integer taskId);

    @Update("UPDATE message SET instance_id = NULL WHERE instance_id = #{instanceId}")
    void clearMessageInstanceReference(@Param("instanceId") Integer instanceId);

    @Update("UPDATE message SET sender_id = NULL WHERE sender_id = #{userId}")
    void clearMessageSenderReference(@Param("userId") Integer userId);

    @Update("UPDATE system_config SET updated_by = NULL WHERE updated_by = #{userId}")
    void clearSystemConfigUpdater(@Param("userId") Integer userId);
}
