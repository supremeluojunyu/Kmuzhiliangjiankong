-- 测试用户（密码均为 admin123）
-- 材料提交组用户，各学院一名
INSERT IGNORE INTO `user` (`user_id`, `name`, `college_id`, `account`, `password`, `status`) VALUES
(2, '张老师', 1, 'teacher_info', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1),
(3, '李老师', 2, 'teacher_econ', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1),
(4, '王老师', 3, 'teacher_art', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1),
(5, '赵老师', 4, 'teacher_sci', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1),
(6, '专家甲', 1, 'expert1', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1),
(7, '信息学院管理员', 1, 'dean_info', '$2b$10$3Lop..eXpleDBP5vLoP13.NtYezBfIzKPy6093M0ec6YQNX0SmmRG', 1);

INSERT IGNORE INTO `user_group` (`user_id`, `group_id`, `is_default`, `sort_order`) VALUES
(2, 5, 1, 0),
(3, 5, 1, 0),
(4, 5, 1, 0),
(5, 5, 1, 0),
(6, 4, 1, 0),
(6, 7, 0, 1),
(7, 3, 1, 0);

INSERT IGNORE INTO `user_session_pref` (`user_id`, `current_group_id`) VALUES
(2, 5), (3, 5), (4, 5), (5, 5), (6, 4), (7, 3);
