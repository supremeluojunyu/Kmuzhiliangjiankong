package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.CollegeManageVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateCollegeRequest;
import com.uqm.dto.UpdateCollegeRequest;
import com.uqm.security.LoginUser;
import com.uqm.service.CollegeManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colleges/manage")
@RequiredArgsConstructor
public class CollegeManageController {

    private final CollegeManageService collegeManageService;

    @GetMapping
    public ApiResponse<List<CollegeManageVo>> list(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(collegeManageService.listAll(user));
    }

    @PostMapping
    public ApiResponse<CollegeManageVo> create(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody CreateCollegeRequest request) {
        return ApiResponse.ok(collegeManageService.create(user, request));
    }

    @PutMapping("/{collegeId}")
    public ApiResponse<CollegeManageVo> update(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer collegeId,
            @RequestBody UpdateCollegeRequest request) {
        return ApiResponse.ok(collegeManageService.update(user, collegeId, request));
    }

    @DeleteMapping("/{collegeId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer collegeId) {
        collegeManageService.delete(user, collegeId);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteResultVo> batchDelete(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ConfirmDeleteRequest request) {
        return ApiResponse.ok(collegeManageService.deleteBatch(user, request));
    }
}
