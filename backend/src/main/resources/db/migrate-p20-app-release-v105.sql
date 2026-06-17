-- P20 移动端 v1.0.5：与 Web 功能对齐（批量删除、级联清理、任务角色权限、响应式菜单）
UPDATE `system_config`
SET `config_value` = JSON_SET(
  COALESCE(`config_value`, '{}'),
  '$.version', '1.0.5',
  '$.versionCode', 6,
  '$.releaseNotes', 'v1.0.5 与 Web 同步\n- 用户/组/任务批量删除与级联清理\n- 组权限：提交材料、只读查看、评分评审\n- 侧边栏/移动端菜单自适应分辨率\n- 系统配置、使用帮助入口优化\n- 操作日志、消息定向发送',
  '$.publishedAt', DATE_FORMAT(NOW(), '%Y-%m-%d'),
  '$.enabled', true
)
WHERE `config_key` = 'app_release';
