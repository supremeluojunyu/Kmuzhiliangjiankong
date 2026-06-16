package com.uqm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("deadline_remind_log")
public class DeadlineRemindLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer instanceId;
    private String remindKey;
    private Integer userId;
    private LocalDateTime sentAt;
}
