package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ImportResultVo {
    private int successCount;
    private int skipCount;
    private int failCount;
    @Builder.Default
    private List<String> messages = new ArrayList<>();
}
