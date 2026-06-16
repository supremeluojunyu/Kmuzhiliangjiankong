-- P18 消息支持按个人定向发送
CREATE TABLE IF NOT EXISTS `message_target_user` (
  `message_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  PRIMARY KEY (`message_id`, `user_id`),
  FOREIGN KEY (`message_id`) REFERENCES `message`(`message_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
