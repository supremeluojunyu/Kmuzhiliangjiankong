-- P22 历史时间修正：服务器原使用 UTC，统一为北京时间 (UTC+8)
-- 不修改 task_definition / task_template 的 config_json 内用户填写的截止日期

UPDATE `college` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `user` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `group` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `user_session_pref` SET `updated_at` = DATE_ADD(`updated_at`, INTERVAL 8 HOUR) WHERE `updated_at` IS NOT NULL;
UPDATE `task_definition` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `task_attachment` SET `uploaded_at` = DATE_ADD(`uploaded_at`, INTERVAL 8 HOUR) WHERE `uploaded_at` IS NOT NULL;
UPDATE `task_allocation` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `task_instance` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `task_instance` SET `completed_at` = DATE_ADD(`completed_at`, INTERVAL 8 HOUR) WHERE `completed_at` IS NOT NULL;
UPDATE `node_record` SET `start_time` = DATE_ADD(`start_time`, INTERVAL 8 HOUR) WHERE `start_time` IS NOT NULL;
UPDATE `node_record` SET `end_time` = DATE_ADD(`end_time`, INTERVAL 8 HOUR) WHERE `end_time` IS NOT NULL;
UPDATE `message` SET `send_time` = DATE_ADD(`send_time`, INTERVAL 8 HOUR) WHERE `send_time` IS NOT NULL;
UPDATE `operation_log` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `task_template` SET `created_at` = DATE_ADD(`created_at`, INTERVAL 8 HOUR) WHERE `created_at` IS NOT NULL;
UPDATE `task_template` SET `updated_at` = DATE_ADD(`updated_at`, INTERVAL 8 HOUR) WHERE `updated_at` IS NOT NULL;
UPDATE `system_config` SET `updated_at` = DATE_ADD(`updated_at`, INTERVAL 8 HOUR) WHERE `updated_at` IS NOT NULL;
UPDATE `deadline_remind_log` SET `sent_at` = DATE_ADD(`sent_at`, INTERVAL 8 HOUR) WHERE `sent_at` IS NOT NULL;
