-- 高校质量监控管理信息系统 - 数据库初始化

CREATE TABLE IF NOT EXISTS `college` (
  `college_id` INT PRIMARY KEY AUTO_INCREMENT,
  `college_name` VARCHAR(100) NOT NULL,
  `college_code` VARCHAR(20) UNIQUE,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  `user_id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `college_id` INT,
  `account` VARCHAR(50) UNIQUE NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `email` VARCHAR(100) NULL COMMENT '通知邮箱',
  `wechat_user_id` VARCHAR(64) NULL COMMENT '企业微信 UserId',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`college_id`) REFERENCES `college`(`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group` (
  `group_id` INT PRIMARY KEY AUTO_INCREMENT,
  `group_name` VARCHAR(50) NOT NULL,
  `parent_group_id` INT NULL,
  `description` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_group` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `group_id` INT NOT NULL,
  `is_default` TINYINT DEFAULT 0,
  `sort_order` INT DEFAULT 0,
  UNIQUE KEY `uk_user_group` (`user_id`, `group_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `group`(`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_session_pref` (
  `user_id` INT PRIMARY KEY,
  `current_group_id` INT NOT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`),
  FOREIGN KEY (`current_group_id`) REFERENCES `group`(`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `permission` (
  `permission_id` INT PRIMARY KEY AUTO_INCREMENT,
  `permission_code` VARCHAR(50) UNIQUE NOT NULL,
  `permission_name` VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group_permission` (
  `group_id` INT NOT NULL,
  `permission_id` INT NOT NULL,
  PRIMARY KEY (`group_id`, `permission_id`),
  FOREIGN KEY (`group_id`) REFERENCES `group`(`group_id`),
  FOREIGN KEY (`permission_id`) REFERENCES `permission`(`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_definition` (
  `task_id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `config_json` JSON,
  `status` VARCHAR(20) DEFAULT 'draft',
  `creator_id` INT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`creator_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_attachment` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_id` INT NOT NULL,
  `file_name` VARCHAR(255),
  `file_path` VARCHAR(500),
  `file_size` BIGINT,
  `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `task_definition`(`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_allocation` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_id` INT NOT NULL,
  `allocation_type` VARCHAR(20) NOT NULL,
  `params_json` JSON,
  `created_by` INT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `task_definition`(`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_instance` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_definition_id` INT NOT NULL,
  `target_group_id` INT NOT NULL,
  `assigned_to_user_id` INT NOT NULL,
  `college_id` INT,
  `status` VARCHAR(20) DEFAULT 'pending',
  `current_node_id` VARCHAR(50),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `completed_at` DATETIME NULL,
  FOREIGN KEY (`task_definition_id`) REFERENCES `task_definition`(`task_id`),
  FOREIGN KEY (`assigned_to_user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `node_record` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_instance_id` INT NOT NULL,
  `node_id` VARCHAR(50) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `submit_data` JSON,
  `start_time` DATETIME,
  `end_time` DATETIME,
  FOREIGN KEY (`task_instance_id`) REFERENCES `task_instance`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `message` (
  `message_id` INT PRIMARY KEY AUTO_INCREMENT,
  `sender_id` INT,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `message_type` VARCHAR(20) DEFAULT 'broadcast',
  `task_id` INT NULL,
  `instance_id` INT NULL,
  `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`sender_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `message_target_group` (
  `message_id` INT NOT NULL,
  `group_id` INT NOT NULL,
  PRIMARY KEY (`message_id`, `group_id`),
  FOREIGN KEY (`message_id`) REFERENCES `message`(`message_id`),
  FOREIGN KEY (`group_id`) REFERENCES `group`(`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `message_target_user` (
  `message_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  PRIMARY KEY (`message_id`, `user_id`),
  FOREIGN KEY (`message_id`) REFERENCES `message`(`message_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `message_read_status` (
  `message_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `is_read` TINYINT DEFAULT 0,
  PRIMARY KEY (`message_id`, `user_id`),
  FOREIGN KEY (`message_id`) REFERENCES `message`(`message_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT,
  `group_id` INT,
  `action` VARCHAR(100),
  `target_type` VARCHAR(50),
  `target_id` INT,
  `detail` JSON,
  `ip` VARCHAR(45),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `system_config` (
  `config_key` VARCHAR(50) PRIMARY KEY,
  `config_value` JSON NOT NULL,
  `updated_by` INT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`updated_by`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
