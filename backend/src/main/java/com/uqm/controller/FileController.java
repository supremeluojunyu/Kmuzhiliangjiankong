package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.BatchDownloadRequest;
import com.uqm.dto.FileUploadVo;
import com.uqm.service.FilePreviewService;
import com.uqm.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final FilePreviewService filePreviewService;

    @PostMapping("/upload")
    public ApiResponse<FileUploadVo> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileStorageService.store(file));
    }

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<String> preview(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(filePreviewService.toPreviewHtml(fileName));
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        Resource resource = fileStorageService.load(fileName);
        MediaType mediaType = fileStorageService.resolveMediaType(fileName);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .body(resource);
    }

    @PostMapping("/batch-download")
    public ResponseEntity<Resource> batchDownload(@Valid @RequestBody BatchDownloadRequest request) {
        byte[] zipBytes = fileStorageService.packZip(request);
        String zipName = request.getZipName() != null && !request.getZipName().isBlank()
                ? request.getZipName() : "materials.zip";
        if (!zipName.endsWith(".zip")) {
            zipName = zipName + ".zip";
        }
        String encoded = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(zipBytes));
    }
}
