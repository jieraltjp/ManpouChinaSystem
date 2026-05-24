-- v2.0: 国内报关新增用户可编辑报关申报号字段
-- customsCode = 系统流水号（DC-YYYYMMDD-NNN），不可编辑
-- customsDeclarationNo = 用户自填报关单号，可编辑，支持批量修改

ALTER TABLE domestic_customs_record
  ADD COLUMN customs_declaration_no VARCHAR(64) DEFAULT NULL COMMENT '报关申报号（用户自填）' AFTER customs_code;

CREATE INDEX idx_dc_declaration_no ON domestic_customs_record (customs_declaration_no);
