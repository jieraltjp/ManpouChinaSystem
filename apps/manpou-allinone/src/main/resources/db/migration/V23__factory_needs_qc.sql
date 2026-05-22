-- V23: factory.needs_qc 字段（老厂家免验功能，SPEC-B13）
-- 默认 1=需要验货，向后兼容已有工厂

ALTER TABLE factory
  ADD COLUMN needs_qc TINYINT(1) NOT NULL DEFAULT 1
  COMMENT '是否需要验货：1=需要，0=老厂家免验'
  AFTER cooperation_status;
