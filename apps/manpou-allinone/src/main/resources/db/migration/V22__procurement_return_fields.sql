-- V22: 新增退货字段（订货失败功能）
-- 新增 return_reason VARCHAR(512) 记录退货原因
-- 新增 return_date DATETIME 记录退货时间

ALTER TABLE procurement
  ADD COLUMN return_reason VARCHAR(512) DEFAULT NULL AFTER status;

ALTER TABLE procurement
  ADD COLUMN return_date DATETIME DEFAULT NULL AFTER return_reason;
