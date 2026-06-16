-- P14 系统配置（认证 / 通知 / 数据保留）
CREATE TABLE IF NOT EXISTS `system_config` (
  `config_key` VARCHAR(50) PRIMARY KEY,
  `config_value` JSON NOT NULL,
  `updated_by` INT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`updated_by`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `permission` (`permission_code`, `permission_name`) VALUES
('system:config', '系统配置管理');

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 1, permission_id FROM `permission` WHERE permission_code = 'system:config';

INSERT IGNORE INTO `system_config` (`config_key`, `config_value`) VALUES
('auth', '{"enabled":false,"provider":"local","localLoginEnabled":true,"frontendBaseUrl":"http://localhost:5173","casServerUrl":"","casLoginPath":"/login","serviceUrl":"","oauthIssuer":"","oauthClientId":"","oauthClientSecret":"","oauthRedirectUri":"","oauthScope":"openid profile email","autoProvision":false,"defaultGroupId":6}'),
('notification', '{"emailEnabled":false,"smtpHost":"","smtpPort":587,"smtpUsername":"","smtpPassword":"","smtpFrom":"","smtpSsl":true,"wechatEnabled":false,"wechatCorpId":"","wechatAgentId":"","wechatSecret":"","notifyOnTaskPublish":true,"notifyOnMessageBroadcast":false,"notifyOnDeadline":false,"deadlineRemindDays":3}'),
('retention', '{"enabled":false,"taskDataYears":5,"messageDataYears":3,"logDataYears":2,"runHour":3,"lastRunAt":null,"lastRunSummary":""}');

-- 用户通知字段（若已存在可忽略报错）
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100) NULL COMMENT '通知邮箱';
ALTER TABLE `user` ADD COLUMN `wechat_user_id` VARCHAR(64) NULL COMMENT '企业微信 UserId';
