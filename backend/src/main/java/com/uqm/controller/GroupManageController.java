package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateGroupRequest;
import com.uqm.dto.GroupManageVo;
import com.uqm.dto.UpdateGroupRequest;
import com.uqm.entity.Permission;
import com.uqm.security.LoginUser;
import com.uqm.service.GroupManageService;
import com.uqm.service.GroupImportService;
import com.uqm.service.GroupManageService;
import com.uqm.dto.ImportResultVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/groups/manage")
@RequiredArgsConstructor
public class GroupManageController {

    private final GroupManageService groupManageService;
    private final GroupImportService groupImportService;

    @GetMapping
    public ApiResponse<List<GroupManageVo>> list(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(groupManageService.listAll(user));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> permissions(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(groupManageService.listPermissions(user));
    }

    @PostMapping
    public ApiResponse<GroupManageVo> create(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.ok(groupManageService.create(user, request));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupManageVo> update(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer groupId,
            @RequestBody UpdateGroupRequest request) {
        return ApiResponse.ok(groupManageService.update(user, groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer groupId) {
        groupManageService.delete(user, groupId);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteResultVo> batchDelete(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ConfirmDeleteRequest request) {
        return ApiResponse.ok(groupManageService.deleteBatch(user, request));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate(@AuthenticationPrincipal LoginUser user) {
        groupManageService.listAll(user);
        byte[] body = groupImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=group-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResultVo> importGroups(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(groupImportService.importGroups(user, file));
    }
}
