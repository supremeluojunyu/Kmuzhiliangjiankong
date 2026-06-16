package com.uqm.dto;

import lombok.Data;

@Data
public class TestNotificationRequest {
    private String channel;
    private String target;
}
