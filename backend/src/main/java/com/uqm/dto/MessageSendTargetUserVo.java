package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageSendTargetUserVo {
    private Integer userId;
    private String name;
    private String account;
}
