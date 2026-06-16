package com.uqm.mapper;

import com.uqm.dto.CollegeStatRow;
import com.uqm.dto.NodeStatRow;
import com.uqm.dto.ScoreRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatMapper {

    @Select("""
            SELECT COUNT(*) FROM task_instance ti
            WHERE ti.task_definition_id = #{taskId}
              AND (#{collegeId} IS NULL OR ti.college_id = #{collegeId})
            """)
    long countInstances(@Param("taskId") Integer taskId, @Param("collegeId") Integer collegeId);

    @Select("""
            SELECT COUNT(*) FROM task_instance ti
            WHERE ti.task_definition_id = #{taskId}
              AND ti.status = 'completed'
              AND (#{collegeId} IS NULL OR ti.college_id = #{collegeId})
            """)
    long countCompletedInstances(@Param("taskId") Integer taskId, @Param("collegeId") Integer collegeId);

    @Select("""
            SELECT ti.college_id AS collegeId, c.college_name AS collegeName,
                   COUNT(*) AS total,
                   SUM(CASE WHEN ti.status = 'completed' THEN 1 ELSE 0 END) AS completed
            FROM task_instance ti
            LEFT JOIN college c ON c.college_id = ti.college_id
            WHERE ti.task_definition_id = #{taskId}
              AND (#{collegeId} IS NULL OR ti.college_id = #{collegeId})
            GROUP BY ti.college_id, c.college_name
            ORDER BY ti.college_id
            """)
    List<CollegeStatRow> collegeStats(@Param("taskId") Integer taskId, @Param("collegeId") Integer collegeId);

    @Select("""
            SELECT nr.node_id AS nodeId,
                   SUM(CASE WHEN nr.status = 'completed' THEN 1 ELSE 0 END) AS completed,
                   COUNT(*) AS total
            FROM node_record nr
            JOIN task_instance ti ON ti.id = nr.task_instance_id
            WHERE ti.task_definition_id = #{taskId}
              AND (#{collegeId} IS NULL OR ti.college_id = #{collegeId})
            GROUP BY nr.node_id
            """)
    List<NodeStatRow> nodeStats(@Param("taskId") Integer taskId, @Param("collegeId") Integer collegeId);

    @Select("""
            SELECT nr.id AS recordId, nr.node_id AS nodeId, nr.submit_data AS submitData,
                   ti.id AS instanceId, u.name AS userName, c.college_name AS collegeName,
                   c.college_id AS collegeId
            FROM node_record nr
            JOIN task_instance ti ON ti.id = nr.task_instance_id
            JOIN user u ON u.user_id = ti.assigned_to_user_id
            LEFT JOIN college c ON c.college_id = ti.college_id
            WHERE ti.task_definition_id = #{taskId}
              AND nr.status = 'completed'
              AND nr.submit_data IS NOT NULL
              AND (#{collegeId} IS NULL OR ti.college_id = #{collegeId})
            """)
    List<ScoreRecordRow> scoreRecords(@Param("taskId") Integer taskId, @Param("collegeId") Integer collegeId);
}
