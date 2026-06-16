package com.uqm.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDownloadRequest {

    @NotEmpty
    private List<FileItem> files;

    private String zipName;

    @Data
    public static class FileItem {
        private String path;
        private String name;
    }
}
