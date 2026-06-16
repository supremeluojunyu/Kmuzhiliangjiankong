package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.dto.FileUploadVo;
import com.uqm.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "zip");

    private final StorageProvider storageProvider;

    public FileUploadVo store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalName.contains("..")) {
            throw new BusinessException(400, "非法文件名");
        }
        String ext = extractExt(originalName);
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException(400, "不支持的文件类型：" + ext);
        }

        String storedKey = UUID.randomUUID() + "." + ext;
        storageProvider.store(file, storedKey);

        return FileUploadVo.builder()
                .fileName(originalName)
                .filePath(storedKey)
                .fileSize(file.getSize())
                .url("/api/files/" + storedKey)
                .build();
    }

    public org.springframework.core.io.Resource load(String fileName) {
        String safe = StringUtils.cleanPath(fileName);
        if (safe.contains("..")) {
            throw new BusinessException(400, "非法文件名");
        }
        return storageProvider.load(safe);
    }

    private String extractExt(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : "";
    }
}
