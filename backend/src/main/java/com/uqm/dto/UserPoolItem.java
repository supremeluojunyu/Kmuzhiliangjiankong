package com.uqm.dto;

import lombok.Data;

@Data
public class UserPoolItem {
    private Integer userId;
    private Integer collegeId;
    private String name;
    private String account;
}
