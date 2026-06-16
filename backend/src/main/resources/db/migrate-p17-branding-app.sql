-- P17 品牌与 APP 发布默认配置
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`) VALUES
('branding', '{"siteName":"昆明学院质量监控任务管理系统","siteShortName":"昆明学院质控","siteSubtitle":"昆明学院","logoUrl":null,"faviconUrl":null,"primaryColor":"#1677ff","loginBackground":"linear-gradient(135deg, #667eea 0%, #764ba2 100%)","downloadPageTitle":"昆明学院质量监控任务管理系统","downloadPageDescription":"扫码或点击下方按钮，下载昆明学院质量监控任务管理系统 Android 客户端","defaultServerUrl":"http://124.220.4.69:5555"}'),
('app_release', '{"version":"1.0.0","versionCode":1,"apkUrl":"http://124.220.4.69:5555/api/public/apk/download","releaseNotes":"首个移动端版本\\n- 我的任务\\n- 消息中心\\n- 使用帮助\\n- 任务/用户/组管理","publishedAt":"2026-06-16","minAndroidVersion":"7.0","enabled":true}');
