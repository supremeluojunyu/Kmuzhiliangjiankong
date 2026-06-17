package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.OperationLogVo;
import com.uqm.security.LoginUser;
import com.uqm.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ApiResponse<PageResult<OperationLogVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        return ApiResponse.ok(operationLogService.list(user, page, pageSize, action, userName, dateFrom, dateTo));
    }

    @GetMapping("/actions")
    public ApiResponse<List<String>> actions(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(operationLogService.listActionTypes(user));
    }
}
