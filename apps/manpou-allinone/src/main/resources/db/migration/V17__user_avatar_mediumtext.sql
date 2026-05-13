-- V17: 将 user.avatar_url 从 VARCHAR(512) 扩展为 MEDIUMTEXT
-- 原因：头像上传经 Canvas 压缩（200×200 JPEG@0.75）后 base64 约 25~30KB
-- MEDIUMTEXT 上限 16MB，绰绰有余

ALTER TABLE `user` MODIFY COLUMN `avatar_url` MEDIUMTEXT DEFAULT NULL COMMENT '头像Base64或URL';
