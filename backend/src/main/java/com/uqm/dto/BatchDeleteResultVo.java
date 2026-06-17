package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchDeleteResultVo {
    private int deletedCount;
    private List<String> errors;
}
