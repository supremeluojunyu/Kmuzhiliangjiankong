package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.entity.College;
import com.uqm.entity.UserGroupEntity;
import com.uqm.security.LoginUser;
import com.uqm.service.CollegeService;
import com.uqm.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {

    private final CollegeService collegeService;

    @GetMapping
    public ApiResponse<List<College>> list(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(collegeService.listForUser(user));
    }
}

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ApiResponse<List<UserGroupEntity>> list() {
        return ApiResponse.ok(groupService.listAll());
    }
}
