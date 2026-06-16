-- 初始化学院
INSERT INTO `college` (`college_name`, `college_code`) VALUES
('信息学院', 'INFO'),
('经管学院', 'ECON'),
('艺术学院', 'ART'),
('理学院', 'SCI');

-- 初始化用户组
INSERT INTO `group` (`group_id`, `group_name`, `description`) VALUES
(1, '系统管理员组', '全部权限'),
(2, '校级管理组', '全校范围管理'),
(3, '院级管理组', '本院范围管理'),
(4, '专家/评审组', '评分与评审'),
(5, '材料提交组', '提交材料'),
(6, '查看组', '只读查看'),
(7, '评分组', '评分');

-- 初始化权限
INSERT INTO `permission` (`permission_code`, `permission_name`) VALUES
('task:create', '创建任务'),
('task:config', '配置流程'),
('task:allocate', '分配任务'),
('stat:view_all', '查看全校统计'),
('stat:view_college', '查看本院统计'),
('message:send', '发送组消息'),
('data:export', '导出数据'),
('user:manage', '用户管理'),
('group:manage', '组管理'),
('system:config', '系统配置管理');

-- 系统管理员组拥有全部权限
INSERT INTO `group_permission` (`group_id`, `permission_id`)
SELECT 1, permission_id FROM `permission`;

-- 初始化系统配置默认值
INSERT INTO `system_config` (`config_key`, `config_value`) VALUES
('auth', '{"enabled":false,"provider":"local","localLoginEnabled":true,"casServerUrl":"","casLoginPath":"/login","serviceUrl":"","oauthIssuer":"","oauthClientId":"","oauthClientSecret":"","oauthRedirectUri":"","oauthScope":"openid profile email","autoProvision":false,"defaultGroupId":6}'),
('notification', '{"emailEnabled":false,"smtpHost":"","smtpPort":587,"smtpUsername":"","smtpPassword":"","smtpFrom":"","smtpSsl":true,"wechatEnabled":false,"wechatCorpId":"","wechatAgentId":"","wechatSecret":"","notifyOnTaskPublish":true,"notifyOnMessageBroadcast":false,"notifyOnDeadline":false,"deadlineRemindDays":3}'),
('retention', '{"enabled":false,"taskDataYears":5,"messageDataYears":3,"logDataYears":2,"runHour":3,"lastRunAt":null,"lastRunSummary":""}'),
('storage', '{"type":"local","localPath":"./data/uploads","s3Endpoint":"","s3Region":"us-east-1","s3Bucket":"","s3AccessKey":"","s3SecretKey":"","s3PathStyleAccess":true}');

-- 校级管理组权限
INSERT INTO `group_permission` (`group_id`, `permission_id`)
SELECT 2, permission_id FROM `permission` WHERE permission_code != 'group:manage';

-- 院级管理组权限（本院范围）
INSERT INTO `group_permission` (`group_id`, `permission_id`)
SELECT 3, permission_id FROM `permission`
WHERE permission_code IN ('task:create', 'task:allocate', 'message:send', 'data:export', 'stat:view_college', 'user:manage');

-- 默认管理员 admin / admin123 (BCrypt)
INSERT INTO `user` (`name`, `college_id`, `account`, `password`, `status`) VALUES
('系统管理员', 1, 'admin', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1);

INSERT INTO `user_group` (`user_id`, `group_id`, `is_default`, `sort_order`) VALUES
(1, 1, 1, 0),
(1, 2, 0, 1);

INSERT INTO `user_session_pref` (`user_id`, `current_group_id`) VALUES (1, 1);

-- 测试用户（密码均为 admin123，详见 seed-test-users.sql）
