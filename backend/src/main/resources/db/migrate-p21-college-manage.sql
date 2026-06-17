-- P21 学院管理权限
INSERT IGNORE INTO `permission` (`permission_code`, `permission_name`) VALUES
('college:manage', '学院管理');

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 1, permission_id FROM `permission` WHERE permission_code = 'college:manage';

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 2, permission_id FROM `permission`
WHERE permission_code = 'college:manage'
AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 2 AND gp.permission_id = permission.permission_id
);
