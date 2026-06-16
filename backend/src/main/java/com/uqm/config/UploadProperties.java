package com.uqm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "uqm.upload")
public class UploadProperties {
    /** local | s3 */
    private String type = "local";
    private String path = "./data/uploads";
    private S3 s3 = new S3();

    @Data
    public static class S3 {
        private String endpoint;
        private String region = "us-east-1";
        private String bucket;
        private String accessKey;
        private String secretKey;
        /** 路径风格访问（MinIO / 部分 OSS 需要 true） */
        private boolean pathStyleAccess = true;
    }
}
