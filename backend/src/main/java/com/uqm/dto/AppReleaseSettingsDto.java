package com.uqm.dto;

import lombok.Data;

@Data
public class AppReleaseSettingsDto {
    private String version = "1.0.0";
    private Integer versionCode = 1;
    private String apkUrl;
    private String releaseNotes;
    private String publishedAt;
    private String minAndroidVersion = "7.0";
    private boolean enabled = true;
}
