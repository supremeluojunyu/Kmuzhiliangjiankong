-- P16 文件存储配置（system_config）
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`) VALUES
('storage', '{"type":"local","localPath":"./data/uploads","s3Endpoint":"","s3Region":"us-east-1","s3Bucket":"","s3AccessKey":"","s3SecretKey":"","s3PathStyleAccess":true}');
