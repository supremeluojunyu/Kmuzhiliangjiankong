package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeProgressVo {
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private long completed;
    private long total;
    private double rate;
}
