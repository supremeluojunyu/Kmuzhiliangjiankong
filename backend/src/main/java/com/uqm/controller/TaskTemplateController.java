package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.CreateFromTemplateRequest;
import com.uqm.dto.SaveTemplateRequest;
import com.uqm.dto.TaskTemplateVo;
import com.uqm.dto.TaskVo;
import com.uqm.security.LoginUser;
import com.uqm.service.TaskTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/task/templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService templateService;

    @GetMapping
    public ApiResponse<List<TaskTemplateVo>> list(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(templateService.list(user));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TaskTemplateVo> detail(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer templateId) {
        return ApiResponse.ok(templateService.get(user, templateId));
    }

    @PostMapping
    public ApiResponse<TaskTemplateVo> save(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody SaveTemplateRequest request) {
        return ApiResponse.ok(templateService.save(user, request));
    }

    @PostMapping("/from-task/{taskId}")
    public ApiResponse<TaskTemplateVo> saveFromTask(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId,
            @Valid @RequestBody SaveTemplateRequest request) {
        return ApiResponse.ok(templateService.saveFromTask(user, taskId, request));
    }

    @PostMapping("/{templateId}/create-task")
    public ApiResponse<TaskVo> createTask(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer templateId,
            @RequestBody(required = false) CreateFromTemplateRequest request) {
        if (request == null) {
            request = new CreateFromTemplateRequest();
        }
        return ApiResponse.ok(templateService.createTaskFromTemplate(user, templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer templateId) {
        templateService.delete(user, templateId);
        return ApiResponse.ok(null);
    }
}
