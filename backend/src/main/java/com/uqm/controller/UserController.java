package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.GroupDto;
import com.uqm.dto.SwitchGroupRequest;
import com.uqm.dto.UserProfileResponse;
import com.uqm.security.LoginUser;
import com.uqm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/groups")
    public ApiResponse<List<GroupDto>> groups(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(authService.listGroups(user.getUserId()));
    }

    @PostMapping("/switch-group")
    public ApiResponse<UserProfileResponse> switchGroup(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody SwitchGroupRequest request) {
        return ApiResponse.ok(authService.switchGroup(user, request));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> profile(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(authService.getProfile(user.getUserId(), user.getCurrentGroupId()));
    }
}
