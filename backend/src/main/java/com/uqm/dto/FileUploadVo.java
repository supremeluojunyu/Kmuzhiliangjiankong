package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadVo {
    private String fileName;
    private String filePath;
    private long fileSize;
    private String url;
}
