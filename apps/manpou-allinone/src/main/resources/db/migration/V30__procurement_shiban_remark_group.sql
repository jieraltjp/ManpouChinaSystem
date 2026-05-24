-- V30: 追加发注单 支番/备注/团体 字段
-- 2026-05-24

ALTER TABLE `procurement`
  ADD COLUMN `shiban`           VARCHAR(64)  DEFAULT NULL COMMENT '支番' AFTER `sub_product_code`,
  ADD COLUMN `remark`           VARCHAR(512) DEFAULT NULL COMMENT '备注' AFTER `carton_notes`,
  ADD COLUMN `group_`           VARCHAR(128) DEFAULT NULL COMMENT '团体' AFTER `remark`;

CREATE INDEX idx_proc_shiban ON `procurement` (`shiban`);
