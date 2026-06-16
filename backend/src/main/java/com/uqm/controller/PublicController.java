package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.common.BusinessException;
import com.uqm.dto.AppReleaseSettingsDto;
import com.uqm.dto.BrandingSettingsDto;
import com.uqm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final SystemConfigService systemConfigService;

    @Value("${uqm.apk.path:/home/golden/university-quality-monitor/data/apk/uqmonitor-latest.apk}")
    private String apkPath;

    @GetMapping("/branding")
    public ApiResponse<BrandingSettingsDto> branding() {
        return ApiResponse.ok(systemConfigService.getPublicBranding());
    }

    @GetMapping("/app-release")
    public ApiResponse<AppReleaseSettingsDto> appRelease() {
        return ApiResponse.ok(systemConfigService.getPublicAppRelease());
    }

    @GetMapping("/apk/download")
    public ResponseEntity<Resource> downloadApk() {
        Path file = Path.of(apkPath);
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(404, "APK 文件尚未就绪，请先在 GitHub Actions 构建后上传至 " + apkPath);
        }
        String fileName = "uqmonitor-" + systemConfigService.getPublicAppRelease().getVersion() + ".apk";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }
}
