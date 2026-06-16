package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.ScoreSummaryVo;
import com.uqm.dto.TaskProgressVo;
import com.uqm.security.LoginUser;
import com.uqm.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/task-progress/{taskId}")
    public ApiResponse<TaskProgressVo> taskProgress(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId) {
        return ApiResponse.ok(statService.getTaskProgress(user, taskId));
    }

    @GetMapping("/score-summary/{taskId}")
    public ApiResponse<ScoreSummaryVo> scoreSummary(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(statService.getScoreSummary(user, taskId, keyword));
    }

    @GetMapping("/export-reviews/{taskId}")
    public ResponseEntity<byte[]> exportReviews(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Integer taskId,
            @RequestParam(required = false) String keyword) {
        byte[] data = statService.exportReviewsExcel(user, taskId, keyword);
        String filename = URLEncoder.encode("评语汇总_任务" + taskId + ".xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
