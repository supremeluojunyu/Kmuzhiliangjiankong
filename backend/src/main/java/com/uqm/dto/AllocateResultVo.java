package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AllocateResultVo {
    private Integer allocationId;
    private Integer taskId;
    private String allocationType;
    private int createdCount;
    private List<Integer> instanceIds;
}
