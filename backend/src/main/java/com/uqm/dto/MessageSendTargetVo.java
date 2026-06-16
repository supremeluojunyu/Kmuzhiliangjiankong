package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageSendTargetVo {
    private Integer groupId;
    private String groupName;
    private List<MessageSendTargetUserVo> users;
}
