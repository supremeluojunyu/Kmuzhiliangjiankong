package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.CreateUserRequest;
import com.uqm.dto.UpdateUserRequest;
import com.uqm.dto.UserManageVo;
import com.uqm.security.LoginUser;
import com.uqm.service.UserManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping
    public ApiResponse<PageResult<UserManageVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userManageService.list(user, page, pageSize, keyword));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserManageVo> detail(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer userId) {
        return ApiResponse.ok(userManageService.getById(user, userId));
    }

    @PostMapping
    public ApiResponse<UserManageVo> create(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userManageService.create(user, request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserManageVo> update(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer userId,
            @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userManageService.update(user, userId, request));
    }
}
