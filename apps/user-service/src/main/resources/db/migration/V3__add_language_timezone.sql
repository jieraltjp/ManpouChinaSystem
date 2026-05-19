-- V3: 添加 language 和 timezone 列（ProfilePage 个人设置需要）
-- 背景：User 实体已有 language/timezone 字段，但 user_service 数据库 ddl-auto:none，列不存在
ALTER TABLE `user` ADD COLUMN `language` VARCHAR(10) DEFAULT 'zh' COMMENT '界面语言 zh/ja';
ALTER TABLE `user` ADD COLUMN `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '界面时区';
