-- P10 任务模板
CREATE TABLE IF NOT EXISTS `task_template` (
  `template_id` INT PRIMARY KEY AUTO_INCREMENT,
  `template_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `config_json` JSON NOT NULL,
  `creator_id` INT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`creator_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `task_template` (`template_id`, `template_name`, `description`, `config_json`, `creator_id`) VALUES
(1, '课程大纲检查（标准流程）', '材料提交 → 专家评分，适用于常规课程质量检查',
'{"nodes":[{"nodeId":"node_submit","nodeType":"submit","nodeName":"上传大纲","executeGroupId":5,"dependsOn":[],"executionMode":"sequential","timeLimitHours":72},{"nodeId":"node_score","nodeType":"score","nodeName":"专家评分","executeGroupId":4,"dependsOn":["node_submit"],"executionMode":"sequential","timeLimitHours":48}],"globalTimeStart":null,"globalTimeEnd":null}',
1);
