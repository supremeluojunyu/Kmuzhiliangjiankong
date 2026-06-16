package com.uqm.dto;

import lombok.Data;

@Data
public class StorageSettingsDto {
    /** local | s3 */
    private String type = "local";
    private String localPath = "./data/uploads";
    private String s3Endpoint;
    private String s3Region = "us-east-1";
    private String s3Bucket;
    private String s3AccessKey;
    private String s3SecretKey;
    private boolean s3PathStyleAccess = true;
}
