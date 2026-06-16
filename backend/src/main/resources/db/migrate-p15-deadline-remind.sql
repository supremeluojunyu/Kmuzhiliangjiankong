-- P15 截止提醒去重日志
CREATE TABLE IF NOT EXISTS `deadline_remind_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `instance_id` INT NOT NULL,
  `remind_key` VARCHAR(100) NOT NULL,
  `user_id` INT NOT NULL,
  `sent_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_instance_remind` (`instance_id`, `remind_key`),
  FOREIGN KEY (`instance_id`) REFERENCES `task_instance`(`id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
