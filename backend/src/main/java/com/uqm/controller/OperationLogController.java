package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.OperationLogVo;
import com.uqm.security.LoginUser;
import com.uqm.service.OperationLogService;
import com.uqm.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<PageResult<OperationLogVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String action) {
        if (!permissionService.hasPermission(user, "group:manage")
                && !permissionService.hasPermission(user, "user:manage")) {
            permissionService.requirePermission(user, "group:manage");
        }
        return ApiResponse.ok(operationLogService.list(user, page, pageSize, action));
    }
}
