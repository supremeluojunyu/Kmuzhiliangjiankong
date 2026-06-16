package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.dto.BatchDownloadRequest;
import com.uqm.dto.FileUploadVo;
import com.uqm.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public MediaType resolveMediaType(String fileName) {
        String ext = extractExt(fileName).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    public byte[] packZip(BatchDownloadRequest request) {
        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new BusinessException(400, "请选择要下载的文件");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> usedNames = new java.util.HashSet<>();
            for (BatchDownloadRequest.FileItem item : request.getFiles()) {
                if (item == null || !StringUtils.hasText(item.getPath())) {
                    continue;
                }
                String safePath = StringUtils.cleanPath(item.getPath());
                if (safePath.contains("..")) {
                    throw new BusinessException(400, "非法文件名");
                }
                String entryName = StringUtils.hasText(item.getName()) ? item.getName() : safePath;
                entryName = StringUtils.cleanPath(entryName);
                if (entryName.contains("..") || entryName.isEmpty()) {
                    entryName = safePath;
                }
                String uniqueName = entryName;
                int dup = 1;
                while (!usedNames.add(uniqueName)) {
                    int dot = entryName.lastIndexOf('.');
                    if (dot > 0) {
                        uniqueName = entryName.substring(0, dot) + "(" + dup + ")" + entryName.substring(dot);
                    } else {
                        uniqueName = entryName + "(" + dup + ")";
                    }
                    dup++;
                }
                org.springframework.core.io.Resource resource = storageProvider.load(safePath);
                zos.putNextEntry(new ZipEntry(uniqueName));
                try (InputStream in = resource.getInputStream()) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "打包下载失败");
        }
    }

    private String extractExt(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : "";
    }
}
