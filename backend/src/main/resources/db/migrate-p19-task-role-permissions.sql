-- 任务角色权限：提交材料、只读查看、评分评审、审核任务
INSERT IGNORE INTO `permission` (`permission_code`, `permission_name`) VALUES
('task:submit', '提交材料'),
('task:view', '只读查看'),
('task:score', '评分评审'),
('task:approve', '审核任务');

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 4, permission_id FROM `permission`
WHERE permission_code IN ('task:score', 'task:approve', 'message:send', 'stat:view_all', 'data:export')
AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 4 AND gp.permission_id = permission.permission_id
);

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 5, permission_id FROM `permission`
WHERE permission_code IN ('task:submit', 'message:send')
AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 5 AND gp.permission_id = permission.permission_id
);

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 6, permission_id FROM `permission`
WHERE permission_code IN ('task:view', 'stat:view_college')
AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 6 AND gp.permission_id = permission.permission_id
);

INSERT IGNORE INTO `group_permission` (`group_id`, `permission_id`)
SELECT 7, permission_id FROM `permission`
WHERE permission_code IN ('task:score', 'message:send')
AND NOT EXISTS (
    SELECT 1 FROM `group_permission` gp
    WHERE gp.group_id = 7 AND gp.permission_id = permission.permission_id
);
