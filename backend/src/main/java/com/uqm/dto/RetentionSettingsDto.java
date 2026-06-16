package com.uqm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RetentionSettingsDto {
    private boolean enabled;
    private int taskDataYears = 5;
    private int messageDataYears = 3;
    private int logDataYears = 2;
    private int runHour = 3;
    private LocalDateTime lastRunAt;
    private String lastRunSummary;
}
