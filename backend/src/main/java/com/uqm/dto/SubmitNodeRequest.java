package com.uqm.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitNodeRequest {
    private Map<String, Object> submitData;
    private Boolean draft;
}
