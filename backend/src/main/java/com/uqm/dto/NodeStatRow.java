package com.uqm.dto;

import lombok.Data;

@Data
public class NodeStatRow {
    private String nodeId;
    private Long completed;
    private Long total;
}
