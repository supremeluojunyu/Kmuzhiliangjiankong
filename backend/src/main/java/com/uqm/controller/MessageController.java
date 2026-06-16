package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.PageResult;
import com.uqm.dto.CreateTaskRequest;
import com.uqm.dto.MessageSendTargetVo;
import com.uqm.dto.MessageVo;
import com.uqm.dto.SendMessageRequest;
import com.uqm.dto.TaskVo;
import com.uqm.security.LoginUser;
import com.uqm.service.MessageService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<PageResult<MessageVo>> list(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(messageService.listMessages(user, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(Map.of("count", messageService.getUnreadCount(user)));
    }

    @GetMapping("/send-targets")
    public ApiResponse<List<MessageSendTargetVo>> sendTargets(
            @AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(messageService.listSendTargets(user));
    }

    @PostMapping
    public ApiResponse<MessageVo> send(
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(messageService.sendMessage(user, request));
    }

    @PostMapping("/{messageId}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer messageId) {
        messageService.markRead(user, messageId);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal LoginUser user) {
        messageService.markAllRead(user);
        return ApiResponse.ok();
    }
}
