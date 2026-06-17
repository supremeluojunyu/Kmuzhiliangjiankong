package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateUserRequest;
import com.uqm.dto.UpdateUserRequest;
import com.uqm.dto.UserManageVo;
import com.uqm.security.LoginUser;
import com.uqm.service.UserManageService;
import com.uqm.service.UserImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uqm.dto.ImportResultVo;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;
    private final UserImportService userImportService;

    @GetMapping
    public ApiResponse<PageResult<UserManageVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userManageService.list(user, page, pageSize, keyword));
    }

    @GetMapping("/{userId:\\d+}")
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

    @PutMapping("/{userId:\\d+}")
    public ApiResponse<UserManageVo> update(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer userId,
            @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userManageService.update(user, userId, request));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate(@AuthenticationPrincipal LoginUser user) {
        userManageService.list(user, 1, 1, null);
        byte[] body = userImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResultVo> importUsers(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(userImportService.importUsers(user, file));
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteResultVo> batchDelete(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ConfirmDeleteRequest request) {
        return ApiResponse.ok(userManageService.deleteUsers(user, request));
    }
}
