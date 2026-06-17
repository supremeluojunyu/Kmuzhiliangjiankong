package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.AllocateResultVo;
import com.uqm.dto.AllocateTaskRequest;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateTaskRequest;
import com.uqm.dto.MyTaskVo;
import com.uqm.dto.PostInstanceCommentRequest;
import com.uqm.dto.SubmitNodeRequest;
import com.uqm.dto.TaskVo;
import com.uqm.dto.MessageVo;
import com.uqm.security.LoginUser;
import com.uqm.service.TaskAllocationService;
import com.uqm.service.TaskCommentService;
import com.uqm.service.TaskExecutionService;
import com.uqm.service.TaskService;
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
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskAllocationService allocationService;
    private final TaskExecutionService executionService;
    private final TaskCommentService commentService;

    @GetMapping("/my-list")
    public ApiResponse<PageResult<MyTaskVo>> myList(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String filter) {
        return ApiResponse.ok(executionService.listMyTasks(user, page, pageSize, filter));
    }

    @GetMapping("/instance/{instanceId:\\d+}")
    public ApiResponse<MyTaskVo> instanceDetail(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer instanceId) {
        return ApiResponse.ok(executionService.getInstanceDetail(user, instanceId));
    }

    @PostMapping("/allocate")
    public ApiResponse<AllocateResultVo> allocate(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody AllocateTaskRequest request) {
        return ApiResponse.ok(allocationService.allocate(user, request));
    }

    @PostMapping("/submit/{instanceId:\\d+}/{nodeId}")
    public ApiResponse<MyTaskVo> submit(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer instanceId,
            @PathVariable String nodeId,
            @RequestBody SubmitNodeRequest request) {
        return ApiResponse.ok(executionService.submitNode(user, instanceId, nodeId, request));
    }

    @GetMapping("/instance/{instanceId:\\d+}/comments")
    public ApiResponse<java.util.List<MessageVo>> listComments(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer instanceId) {
        return ApiResponse.ok(commentService.listComments(user, instanceId));
    }

    @PostMapping("/instance/{instanceId:\\d+}/comments")
    public ApiResponse<MessageVo> postComment(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer instanceId,
            @Valid @RequestBody PostInstanceCommentRequest request) {
        return ApiResponse.ok(commentService.postComment(user, instanceId, request.getContent()));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<TaskVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(taskService.listTasks(user, page, pageSize, status));
    }

    @GetMapping("/{taskId:\\d+}")
    public ApiResponse<TaskVo> detail(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(taskService.getTask(user, taskId));
    }

    @PostMapping("/create")
    public ApiResponse<TaskVo> create(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskService.createTask(user, request));
    }

    @PutMapping("/{taskId:\\d+}")
    public ApiResponse<TaskVo> update(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskService.updateTask(user, taskId, request));
    }

    @PostMapping("/publish/{taskId:\\d+}")
    public ApiResponse<TaskVo> publish(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(taskService.publishTask(user, taskId));
    }

    @PostMapping("/{taskId:\\d+}/pause")
    public ApiResponse<TaskVo> pause(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(taskService.pauseTask(user, taskId));
    }

    @PostMapping("/{taskId:\\d+}/resume")
    public ApiResponse<TaskVo> resume(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(taskService.resumeTask(user, taskId));
    }

    @PostMapping("/{taskId:\\d+}/stop")
    public ApiResponse<TaskVo> stop(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(taskService.stopTask(user, taskId));
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteResultVo> batchDelete(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ConfirmDeleteRequest request) {
        return ApiResponse.ok(taskService.deleteTasks(user, request));
    }
}
