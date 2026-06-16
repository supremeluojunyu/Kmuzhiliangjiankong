-- P8 院级数据隔离：新增权限并授予院级管理组
INSERT IGNORE INTO `permission` (`permission_code`, `permission_name`) VALUES
('stat:view_college', '查看本院统计');

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 3, permission_id FROM `permission`
WHERE permission_code IN ('stat:view_college', 'user:manage')
  AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 3 AND gp.permission_id = permission.permission_id
  );

-- 院级管理员测试账号 dean_info / admin123（信息学院）
INSERT IGNORE INTO `user` (`user_id`, `name`, `college_id`, `account`, `password`, `status`) VALUES
(7, '信息学院管理员', 1, 'dean_info', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1);

INSERT IGNORE INTO `user_group` (`user_id`, `group_id`, `is_default`, `sort_order`) VALUES
(7, 3, 1, 0);

INSERT IGNORE INTO `user_session_pref` (`user_id`, `current_group_id`) VALUES
(7, 3);
