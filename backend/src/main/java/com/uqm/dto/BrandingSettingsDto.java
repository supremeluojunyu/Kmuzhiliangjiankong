package com.uqm.dto;

import lombok.Data;

@Data
public class BrandingSettingsDto {
    private String siteName = "昆明学院质量监控任务管理系统";
    private String siteShortName = "昆明学院质控";
    private String siteSubtitle = "昆明学院";
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor = "#1677ff";
    private String loginBackground = "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
    private String downloadPageTitle = "昆明学院质量监控任务管理系统";
    private String downloadPageDescription = "扫码或点击下方按钮，下载昆明学院质量监控任务管理系统 Android 客户端";
    /** 移动端默认服务器地址（可在登录页修改） */
    private String defaultServerUrl = "http://124.220.4.69:5555";
}
