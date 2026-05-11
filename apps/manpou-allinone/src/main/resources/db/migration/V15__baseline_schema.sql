-- ============================================================
-- V15__baseline_schema.sql — 生产基准 Schema
-- ============================================================
-- 来源: production_schema.sql (mysqldump 2026-05-11)
-- Flyway baseline: V14 (已应用)
-- 所有表使用 CREATE TABLE IF NOT EXISTS，幂等安全
-- 注意: flyway_schema_history 由 baseline/生产初始化脚本创建，V15 不重复定义
-- 注意: cn_hs_code/jp_hs_code 表仅含表结构；factory(~500行)/product(~5000行)
--       的实际数据由 JPA ddl-auto 在开发期生成，production_schema.sql 为纯 DDL
-- ============================================================

-- -------------------------------------------------------
-- 1. 基础设施表（不含 flyway_schema_history，由 baseline 创建）
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '聚合根类型：Order/Payment',
  `aggregate_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '聚合根 ID',
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '事件类型：OrderCreated/PaymentCompleted',
  `payload` json NOT NULL COMMENT '事件载荷',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待发送，1=发送中，2=已发送，3=发送失败',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retries` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪 ID',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_status_create` (`status`,`create_time`),
  KEY `idx_aggregate` (`aggregate_type`,`aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息 Outbox 表';

CREATE TABLE IF NOT EXISTS `saga_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `saga_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Saga 实例 ID',
  `step_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '步骤名称',
  `step_order` int NOT NULL DEFAULT '0' COMMENT '步骤顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待执行，1=成功，2=补偿中，3=已补偿，4=补偿失败',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪 ID',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_saga_id` (`saga_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saga 执行日志表';

CREATE TABLE IF NOT EXISTS `signing_key` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密钥 ID（写入 JWT kid header）',
  `public_key_pem` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公钥 PEM（不含私钥）',
  `private_key_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '私钥文件路径（不含私钥内容）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=INACTIVE，1=ACTIVE',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '密钥创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_kid` (`kid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RS256 签名密钥表';

-- -------------------------------------------------------
-- 2. 组织架构表
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `company` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_code` varchar(32) NOT NULL,
  `company_name_cn` varchar(128) NOT NULL,
  `company_name_jp` varchar(128) DEFAULT NULL,
  `company_type` varchar(32) DEFAULT NULL,
  `tax_id` varchar(64) DEFAULT NULL,
  `address` varchar(256) DEFAULT NULL,
  `contact_person` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`company_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `dept_code` varchar(32) NOT NULL,
  `dept_name_cn` varchar(64) NOT NULL,
  `dept_name_jp` varchar(64) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_dept` (`company_id`,`dept_code`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `position_code` varchar(32) NOT NULL,
  `position_name_cn` varchar(64) NOT NULL,
  `position_name_jp` varchar(64) DEFAULT NULL,
  `level_` int DEFAULT '0' COMMENT '职级（数字越大越高）',
  `company_id` bigint DEFAULT NULL COMMENT 'NULL=全局职务，非NULL=该公司私有',
  `status` tinyint DEFAULT '1',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `position_code` (`position_code`),
  KEY `idx_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------
-- 3. 用户权限表
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_code` varchar(32) NOT NULL COMMENT '用户编码 U-XXXX',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(255) NOT NULL COMMENT 'BCrypt哈希',
  `name_cn` varchar(64) DEFAULT NULL COMMENT '中文姓名',
  `name_jp` varchar(64) DEFAULT NULL COMMENT '日文姓名',
  `email` varchar(128) NOT NULL,
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像URL',
  `company_id` bigint DEFAULT NULL COMMENT '所属公司 FK → company.id',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门 FK → department.id',
  `customs_code` varchar(64) DEFAULT NULL COMMENT '报关员备案号',
  `customs_license` varchar(128) DEFAULT NULL COMMENT '报关员证号',
  `language` varchar(8) DEFAULT 'zh',
  `timezone` varchar(16) DEFAULT 'CST',
  `status` tinyint DEFAULT '1' COMMENT '1=正常 0=禁用',
  `last_login_time` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
  `registration_status` varchar(16) DEFAULT 'APPROVED' COMMENT 'APPROVED/PENDING/REJECTED',
  `reject_reason` varchar(256) DEFAULT NULL COMMENT '拒绝原因（REJECTED时填写）',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
  `create_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  `update_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_code` (`user_code`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_company` (`company_id`),
  KEY `idx_dept` (`department_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_position` (`user_id`,`position_id`),
  KEY `idx_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(32) NOT NULL,
  `role_name_cn` varchar(64) NOT NULL,
  `role_name_jp` varchar(64) NOT NULL,
  `role_type` varchar(16) DEFAULT NULL COMMENT 'SYSTEM/BUSINESS',
  `description` varchar(256) DEFAULT NULL,
  `is_editable` tinyint DEFAULT '1' COMMENT '0=系统内置不可编辑',
  `status` tinyint DEFAULT '1',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  `update_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_code` varchar(64) NOT NULL,
  `permission_name_cn` varchar(64) NOT NULL,
  `permission_name_jp` varchar(64) NOT NULL,
  `module` varchar(32) NOT NULL,
  `action_` varchar(16) NOT NULL COMMENT 'READ/CREATE/UPDATE/DELETE/APPROVE/START/COMPLETE/EXPORT',
  `description` varchar(256) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  `update_by` varchar(64) NOT NULL DEFAULT 'SYSTEM',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------
-- 4. 审计表
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `user_id` varchar(64) NOT NULL,
  `username` varchar(64) NOT NULL,
  `user_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `company_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `module` varchar(32) NOT NULL COMMENT '模块标识',
  `action` varchar(32) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/STATUS_CHANGE/LOGIN/LOGOUT/REGISTER/REGISTRATION_APPROVED/REGISTRATION_REJECTED/EXPORT',
  `http_method` varchar(8) DEFAULT NULL,
  `http_url` varchar(256) DEFAULT NULL,
  `resource_type` varchar(64) DEFAULT NULL COMMENT '资源类型',
  `resource_id` varchar(64) DEFAULT NULL,
  `resource_code` varchar(64) DEFAULT NULL,
  `detail` json DEFAULT NULL COMMENT '变更详情JSON',
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_module_time` (`module`,`create_time`),
  KEY `idx_resource` (`resource_type`,`resource_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------
-- 5. 示例/静态数据表（hs_code 见 sql/ 目录单独导入）
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `example` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime(6) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cn_hs_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `编码` text COLLATE utf8mb4_unicode_ci,
  `名称` text COLLATE utf8mb4_unicode_ci,
  `备注` text COLLATE utf8mb4_unicode_ci,
  `第一法定单位` text COLLATE utf8mb4_unicode_ci,
  `第二法定单位` text COLLATE utf8mb4_unicode_ci,
  `监管条件` text COLLATE utf8mb4_unicode_ci,
  `普通税率` text COLLATE utf8mb4_unicode_ci,
  `优惠税率` text COLLATE utf8mb4_unicode_ci,
  `出口税率` text COLLATE utf8mb4_unicode_ci,
  `消费税率` text COLLATE utf8mb4_unicode_ci,
  `增值税率` text COLLATE utf8mb4_unicode_ci,
  `申报要素` text COLLATE utf8mb4_unicode_ci,
  `import_batch` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hs_code` (`编码`(12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jp_hs_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `番号` text COLLATE utf8mb4_unicode_ci,
  `品名` text COLLATE utf8mb4_unicode_ci,
  `単位1` text COLLATE utf8mb4_unicode_ci,
  `単位2` text COLLATE utf8mb4_unicode_ci,
  `基本関税率` text COLLATE utf8mb4_unicode_ci,
  `暫定関税率` text COLLATE utf8mb4_unicode_ci,
  `WTO協定関税率` text COLLATE utf8mb4_unicode_ci,
  `特恵関税率` text COLLATE utf8mb4_unicode_ci,
  `特別特恵関税率` text COLLATE utf8mb4_unicode_ci,
  `Singapore関税率` text COLLATE utf8mb4_unicode_ci,
  `Mexico関税率` text COLLATE utf8mb4_unicode_ci,
  `Malaysia関税率` text COLLATE utf8mb4_unicode_ci,
  `Chile関税率` text COLLATE utf8mb4_unicode_ci,
  `Thailand関税率` text COLLATE utf8mb4_unicode_ci,
  `Indonesia関税率` text COLLATE utf8mb4_unicode_ci,
  `Brunei関税率` text COLLATE utf8mb4_unicode_ci,
  `ASEAN関税率` text COLLATE utf8mb4_unicode_ci,
  `Philippines関税率` text COLLATE utf8mb4_unicode_ci,
  `Switzerland関税率` text COLLATE utf8mb4_unicode_ci,
  `VietNam関税率` text COLLATE utf8mb4_unicode_ci,
  `India関税率` text COLLATE utf8mb4_unicode_ci,
  `Peru関税率` text COLLATE utf8mb4_unicode_ci,
  `RCEP日ANZASEAN税率` text COLLATE utf8mb4_unicode_ci,
  `RCEP中国税率` text COLLATE utf8mb4_unicode_ci,
  `RCEP韓国税率` text COLLATE utf8mb4_unicode_ci,
  `import_batch` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hs_code` (`番号`(12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- 6. 业务表 — 基础数据
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `factory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `factory_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `factory_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` enum('TOOLS','TEXTILE','PLASTIC','ELECTRONICS','FURNITURE','AUTO_PARTS','SPORTS','PET','MEDICAL','CRAFTS','CHEMICAL','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `origin` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `county` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `rough_location` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `latitude` decimal(11,8) DEFAULT NULL,
  `contact_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_wechat` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_qq` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cooperation_status` enum('ACTIVE','SUSPENDED','ELIMINATED','POTENTIAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_terms` enum('CASH','NET_30','NET_60','NET_90','CREDIT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `factory_code` (`factory_code`),
  UNIQUE KEY `uk_factory_name` (`factory_name`),
  KEY `idx_factory_code` (`factory_code`),
  KEY `idx_factory_name` (`factory_name`),
  KEY `idx_factory_category` (`category`),
  KEY `idx_factory_cooperation_status` (`cooperation_status`),
  KEY `idx_factory_province` (`province`),
  KEY `idx_factory_city` (`city`),
  KEY `idx_factory_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工厂/厂家信息表';

CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `master_code` varchar(32) NOT NULL,
  `sub_code` varchar(64) DEFAULT NULL,
  `jan_code` varchar(64) DEFAULT NULL,
  `name_zh` varchar(255) DEFAULT NULL,
  `name_en` varchar(255) DEFAULT NULL,
  `name_ja` varchar(128) DEFAULT NULL,
  `category` enum('OEM','ORDINARY','FACTORY_DIRECT') DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `color_name` varchar(64) DEFAULT NULL,
  `material` varchar(64) DEFAULT NULL,
  `material_ja` varchar(255) DEFAULT NULL,
  `origin` varchar(100) DEFAULT NULL,
  `warehouse` varchar(64) DEFAULT NULL,
  `quantities` int DEFAULT NULL,
  `carton_qty` int DEFAULT NULL,
  `unit` varchar(50) DEFAULT NULL,
  `unit_price_rmb` decimal(12,4) DEFAULT NULL,
  `amount_rmb` decimal(14,4) DEFAULT NULL,
  `tax_rate` decimal(5,4) DEFAULT NULL,
  `tax_point` decimal(5,4) DEFAULT NULL,
  `length_cm` decimal(8,2) DEFAULT NULL,
  `width_cm` decimal(8,2) DEFAULT NULL,
  `height_cm` decimal(8,2) DEFAULT NULL,
  `volume_cbm` decimal(10,6) DEFAULT NULL,
  `gross_weight_kg` decimal(10,4) DEFAULT NULL,
  `net_weight_kg` decimal(10,4) DEFAULT NULL,
  `hs_code` varchar(20) DEFAULT NULL,
  `hs_code_jp` varchar(20) DEFAULT NULL,
  `declaration_elements` text,
  `units_per_package` int DEFAULT NULL,
  `package_length_cm` decimal(8,2) DEFAULT NULL,
  `package_width_cm` decimal(8,2) DEFAULT NULL,
  `package_height_cm` decimal(8,2) DEFAULT NULL,
  `package_volume_cbm` decimal(10,6) DEFAULT NULL,
  `package_weight_kg` decimal(10,4) DEFAULT NULL,
  `requires_qc` bit(1) DEFAULT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `remarks` varchar(512) DEFAULT NULL,
  `last_used_date` date DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_master_sub` (`master_code`,`sub_code`),
  KEY `idx_master_code` (`master_code`),
  KEY `idx_hs_code` (`hs_code`),
  KEY `idx_hs_code_jp` (`hs_code_jp`),
  KEY `idx_name_zh` (`name_zh`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_product_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_factory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `supplier_sku` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `factory_id` bigint NOT NULL,
  `is_preferred` bit(1) DEFAULT NULL,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `unit_price_rmb` decimal(12,4) DEFAULT NULL,
  `moq` int DEFAULT NULL,
  `lead_time_days` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_factory` (`product_id`,`factory_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- 7. 业务表 — 发注/补货
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `replenishment_demand` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `demand_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `demand_type` enum('REPLENISHMENT','NEW_PURCHASE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `japan_lead` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `remarks` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `destination` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `linked_procurement_id` bigint DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'v2.0.0: 子型号全称',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `demand_code` (`demand_code`),
  KEY `idx_demand_status` (`status`),
  KEY `idx_demand_type` (`demand_type`),
  KEY `idx_demand_product_code` (`product_code`),
  KEY `idx_demand_is_deleted` (`is_deleted`),
  KEY `idx_demand_quantity` (`quantity`),
  KEY `idx_demand_destination` (`destination`),
  KEY `idx_demand_sub_product_code` (`sub_product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补货需求';

CREATE TABLE IF NOT EXISTS `procurement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `china_lead` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_company` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `destination` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estimated_price_jpy` decimal(14,2) DEFAULT NULL,
  `exchange_rate` decimal(10,4) NOT NULL,
  `factory_ship_date` date DEFAULT NULL,
  `japan_lead` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_date` date DEFAULT NULL,
  `planned_ship_date` date DEFAULT NULL,
  `price_rmb` decimal(12,4) NOT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_lead` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int NOT NULL,
  `status` enum('未定','予定','OEM','発注待','永康','直送','倉庫着','現地検品','検品','エア便','メーカー直送','輸出','通関','日本着','会計','完了','退货') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tax_point` decimal(5,4) NOT NULL,
  `factory_id` bigint DEFAULT NULL COMMENT '???H??ID ?? factory.id',
  `actual_ship_date` date DEFAULT NULL,
  `billing_type` enum('ZHE_LU_KAI_PIAO','CHAO_HUI_TUI_SHUI','NO_REFUND','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customs_remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `instruction_manual` text COLLATE utf8mb4_unicode_ci,
  `material` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requires_qc` bit(1) DEFAULT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `carton_notes` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `after_sales_deadline` date DEFAULT NULL COMMENT '售后截止日（v1.10.0 新增）',
  `lead_time_days` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_procurement_product_code` (`product_code`),
  KEY `idx_procurement_status` (`status`),
  KEY `idx_procurement_create_time` (`create_time`),
  KEY `idx_procurement_factory_id` (`factory_id`),
  KEY `idx_procurement_sub_product_code` (`sub_product_code`),
  KEY `idx_procurement_order_date` (`order_date`),
  KEY `idx_proc_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- procurement_snapshot 表由 V16 单独创建（幂等兜底）

CREATE TABLE IF NOT EXISTS `demand_procurement_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime(6) NOT NULL,
  `allocated_quantity` int NOT NULL,
  `demand_id` bigint NOT NULL,
  `procurement_id` bigint NOT NULL,
  `status` enum('进行中','已完成','已取消') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_demand_procurement` (`demand_id`,`procurement_id`),
  KEY `idx_mapping_demand` (`demand_id`),
  KEY `idx_mapping_procurement` (`procurement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- 8. 业务表 — 验货/出货
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `qc_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qc_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `procurement_id` bigint DEFAULT NULL COMMENT '关联采购单（V43后不再强制关联）',
  `seller_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qc_user_id` bigint DEFAULT NULL,
  `qc_type` enum('ONSITE','REMOTE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qc_date` date DEFAULT NULL,
  `result` enum('PASS','FAIL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','COMPLETED','RETURN_REQUESTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspection_count` int DEFAULT NULL,
  `passed_count` int DEFAULT NULL,
  `defective_count` int DEFAULT NULL,
  `box_count` int DEFAULT NULL,
  `box_length_cm` decimal(8,2) DEFAULT NULL,
  `box_width_cm` decimal(8,2) DEFAULT NULL,
  `box_height_cm` decimal(8,2) DEFAULT NULL,
  `net_weight_per_unit` decimal(10,4) DEFAULT NULL,
  `gross_weight` decimal(10,4) DEFAULT NULL,
  `tax_inclusive_price` decimal(14,2) DEFAULT NULL,
  `material` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tax_refund` bit(1) DEFAULT NULL,
  `qc_standard` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `images` json DEFAULT NULL,
  `destination` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `order_date` date DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `shipment_batch_id` bigint DEFAULT NULL COMMENT '关联出货批次ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qc_code` (`qc_code`),
  KEY `idx_qc_procurement` (`procurement_id`),
  KEY `idx_qc_result` (`result`),
  KEY `idx_qc_date` (`qc_date`),
  KEY `idx_qc_status` (`status`),
  KEY `idx_qc_is_deleted` (`is_deleted`),
  KEY `idx_qc_shipment_batch` (`shipment_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `qc_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qc_record_id` bigint NOT NULL COMMENT '关联验货记录ID',
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'COS存储文件名',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `url` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问URL',
  `size` bigint NOT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传人用户ID',
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除标记',
  `deleted_at` datetime(6) DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人用户ID',
  PRIMARY KEY (`id`),
  KEY `idx_qc_record_id` (`qc_record_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_filename` (`filename`),
  CONSTRAINT `fk_qc_image_qc_record` FOREIGN KEY (`qc_record_id`) REFERENCES `qc_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验货记录图片表';

CREATE TABLE IF NOT EXISTS `shipment_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime(6) NOT NULL,
  `actual_ship_date` date DEFAULT NULL,
  `batch_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `factory_ship_date` date DEFAULT NULL,
  `procurement_id` bigint NOT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shipment_quantity` int NOT NULL,
  `status` enum('待验货','验货中','已验货','已取消') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sb_procurement` (`procurement_id`),
  KEY `idx_sb_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- 9. 业务表 — 物流/报关/财务
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `logistics_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `container_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货柜号（船公司提供，同批次货物填入相同货柜号）',
  `procurement_id` bigint DEFAULT NULL,
  `factory_id` bigint DEFAULT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plan_type` enum('SEA','AIR','CONSOLIDATION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PLANNED','BOOKED','IN_TRANSIT','DELIVERED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `cargo_length_cm` decimal(8,2) DEFAULT NULL,
  `cargo_width_cm` decimal(8,2) DEFAULT NULL,
  `cargo_height_cm` decimal(8,2) DEFAULT NULL,
  `cargo_volume_cbm` decimal(10,6) DEFAULT NULL,
  `cargo_weight_kg` decimal(10,4) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `requires_qc` bit(1) DEFAULT NULL,
  `container_id` bigint DEFAULT NULL,
  `pool_id` bigint DEFAULT NULL,
  `estimated_ship_date` date DEFAULT NULL,
  `actual_ship_date` date DEFAULT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `qc_record_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_logistics_procurement` (`procurement_id`),
  KEY `idx_logistics_status` (`status`),
  KEY `idx_logistics_plan_type` (`plan_type`),
  KEY `idx_logistics_factory` (`factory_id`),
  KEY `idx_lp_product_code` (`product_code`),
  KEY `idx_lp_estimated_ship_date` (`estimated_ship_date`),
  KEY `idx_lp_create_time` (`create_time`),
  KEY `idx_lp_is_deleted` (`is_deleted`),
  KEY `idx_lp_container_no` (`container_no`),
  KEY `idx_logistics_qc_record` (`qc_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `domestic_customs_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customs_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `procurement_id` bigint DEFAULT NULL,
  `logistics_plan_id` bigint DEFAULT NULL,
  `factory_id` bigint DEFAULT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `estimated_value_cny` decimal(14,2) DEFAULT NULL,
  `status` enum('PENDING','SUBMITTED','CLEARED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `container_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domestic_customs_code` (`customs_code`),
  KEY `idx_dc_procurement_id` (`procurement_id`),
  KEY `idx_dc_logistics_plan_id` (`logistics_plan_id`),
  KEY `idx_dc_factory_id` (`factory_id`),
  KEY `idx_dc_product_code` (`product_code`),
  KEY `idx_dc_status` (`status`),
  KEY `idx_dc_create_time` (`create_time`),
  KEY `idx_dc_is_deleted` (`is_deleted`),
  KEY `idx_dc_container_no` (`container_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `japan_customs_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customs_entry_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入境报关号',
  `procurement_id` bigint DEFAULT NULL,
  `domestic_customs_id` bigint DEFAULT NULL,
  `logistics_plan_id` bigint DEFAULT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('PENDING','IN_PROGRESS','CLEARED','FAILED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `arrival_date` date DEFAULT NULL,
  `customs_broker` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `broker_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `broker_contact` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `import_duty_paid` decimal(14,2) DEFAULT NULL,
  `consumption_tax_paid` decimal(14,2) DEFAULT NULL,
  `clearance_date` date DEFAULT NULL,
  `arrival_port` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `declared_weight_kg` decimal(10,4) DEFAULT NULL,
  `declared_volume_cbm` decimal(10,4) DEFAULT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `container_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `factory_id` bigint DEFAULT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jp_customs_entry_no` (`customs_entry_no`),
  KEY `idx_jp_procurement_id` (`procurement_id`),
  KEY `idx_jp_domestic_customs_id` (`domestic_customs_id`),
  KEY `idx_jp_logistics_plan_id` (`logistics_plan_id`),
  KEY `idx_jp_status` (`status`),
  KEY `idx_jp_create_time` (`create_time`),
  KEY `idx_jp_is_deleted` (`is_deleted`),
  KEY `idx_jp_container_no` (`container_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tax_refund_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `refund_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `procurement_id` bigint DEFAULT NULL,
  `japan_customs_id` bigint DEFAULT NULL,
  `status` enum('APPLYING','COMPLETED','NO_REFUND') COLLATE utf8mb4_unicode_ci NOT NULL,
  `billing_type` enum('ZHE_LU_KAI_PIAO','CHAO_HUI_TUI_SHUI','NO_REFUND','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price_rmb` decimal(14,4) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `tax_point` decimal(6,4) DEFAULT NULL,
  `exchange_rate` decimal(10,6) DEFAULT NULL,
  `estimated_refund_rmb` decimal(14,4) DEFAULT NULL,
  `actual_refund_rmb` decimal(14,4) DEFAULT NULL,
  `refund_date` date DEFAULT NULL,
  `refund_bank` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_tr_procurement_id` (`procurement_id`),
  KEY `idx_tr_japan_customs_id` (`japan_customs_id`),
  KEY `idx_tr_status` (`status`),
  KEY `idx_tr_refund_date` (`refund_date`),
  KEY `idx_tr_create_time` (`create_time`),
  KEY `idx_tr_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sales_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `procurement_id` bigint DEFAULT NULL,
  `product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_product_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sales_channel` enum('AMAZON','MERCALI','SELF_SITE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('LISTED','LOW_STOCK','OUT_OF_STOCK','DISCONTINUED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `listing_date` date DEFAULT NULL,
  `initial_stock` int DEFAULT NULL,
  `current_stock` int DEFAULT NULL,
  `safety_stock` int DEFAULT NULL,
  `sales_quantity` int DEFAULT NULL,
  `returned_quantity` int DEFAULT NULL,
  `return_rate` decimal(6,4) DEFAULT NULL,
  `selling_price_jpy` decimal(14,2) DEFAULT NULL,
  `remarks` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_sr_product_code` (`product_code`),
  KEY `idx_sr_procurement_id` (`procurement_id`),
  KEY `idx_sr_status` (`status`),
  KEY `idx_sr_sales_channel` (`sales_channel`),
  KEY `idx_sr_create_time` (`create_time`),
  KEY `idx_sr_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `consolidation_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime(6) NOT NULL,
  `container_threshold_cbm` decimal(10,4) DEFAULT NULL,
  `destination_port` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_count` int DEFAULT NULL,
  `pool_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('OPEN','PENDING','LOADED','SHIPPED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_cbm` decimal(12,4) DEFAULT NULL,
  `total_weight_kg` decimal(12,4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_code` (`pool_code`),
  KEY `idx_pool_status` (`status`),
  KEY `idx_pool_destination` (`destination_port`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `container` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` datetime(6) NOT NULL,
  `arrival_date` date DEFAULT NULL,
  `container_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `container_type` enum('GP20','GP40','HC40','HC45') COLLATE utf8mb4_unicode_ci NOT NULL,
  `departure_date` date DEFAULT NULL,
  `load_date` date DEFAULT NULL,
  `plan_count` int DEFAULT NULL,
  `pool_id` bigint DEFAULT NULL,
  `status` enum('CREATED','LOADED','DEPARTED','ARRIVED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_cbm` decimal(10,4) DEFAULT NULL,
  `total_weight_kg` decimal(12,4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_container_no` (`container_no`),
  KEY `idx_container_status` (`status`),
  KEY `idx_container_pool` (`pool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- 10. 种子数据（幂等 INSERT IGNORE）
-- 大数据量参考表(cn_hs_code/jp_hs_code)见 sql/ 目录单独导入
-- -------------------------------------------------------

-- 公司
INSERT IGNORE INTO company (id, company_code, company_name_cn, company_name_jp, company_type, create_time, update_time)
VALUES (1, 'HAIT-001', '漫普贸易（中国）有限公司', 'マンプ貿易（中国）有限公司', 'TRADER', NOW(3), NOW(3));

-- 部门（company_id=1, parent_id=NULL）
INSERT IGNORE INTO department (id, company_id, dept_code, dept_name_cn, dept_name_jp, parent_id, create_time, update_time)
VALUES
(1, 1, 'IT',     '信息中心', '情報センター', NULL, NOW(3), NOW(3)),
(2, 1, 'PUR',    '采购部',   '調達部',     NULL, NOW(3), NOW(3)),
(3, 1, 'SALES',  '销售部',   '営業部',     NULL, NOW(3), NOW(3));

-- 职务（全局，company_id=NULL）
INSERT IGNORE INTO position (id, position_code, position_name_cn, position_name_jp, level_, company_id, create_time, update_time)
VALUES
(1, 'BOSS',         '总经理',       '社長',        100, NULL, NOW(3), NOW(3)),
(2, 'VP',           '副总经理',     '副社長',       90, NULL, NOW(3), NOW(3)),
(3, 'DIRECTOR',    '总监',        'ディレクター',  80, NULL, NOW(3), NOW(3)),
(4, 'MGR',          '经理',        'マネージャー',  60, NULL, NOW(3), NOW(3)),
(5, 'ASST_MGR',    '主管',        'アシスタントマネージャー', 50, NULL, NOW(3), NOW(3)),
(6, 'SR_STAFF',     '高级专员',    'シニアスタッフ', 40, NULL, NOW(3), NOW(3)),
(7, 'STAFF',        '专员',        'スタッフ',    20, NULL, NOW(3), NOW(3)),
(8, 'INTERN',       '实习生',      'インターン',   10, NULL, NOW(3), NOW(3)),
(9, 'CUSTOMS_REP',  '报关员',      '通関士',       40, NULL, NOW(3), NOW(3));

-- 角色
INSERT IGNORE INTO role (id, role_code, role_name_cn, role_name_jp, role_type, is_editable, status, create_time, update_time, is_deleted, create_by, update_by)
VALUES
(1, 'ADMIN',   '系统管理员', 'システム管理者',    'SYSTEM',   1, 1, NOW(3), NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(2, 'MANAGER', '运营主管',   '運営マネージャー', 'BUSINESS', 1, 1, NOW(3), NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(3, 'OPERATOR','普通运营',   '一般運営者',        'BUSINESS', 1, 1, NOW(3), NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(4, 'VIEWER',  '查看者',     '閲覧者',            'BUSINESS', 1, 1, NOW(3), NOW(3), 0, 'SYSTEM', 'SYSTEM');

-- 权限（74条，V8:64 + V15:8 + V16:1；含重复audit:export行，INSERT IGNORE兜底）
INSERT IGNORE INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by) VALUES
-- demand CRUD
(1,  'demand:read',    '查看补货需求', '補充需要を表示', 'demand',        'READ',    1,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(2,  'demand:create',  '创建补货需求', '補充需要を作成', 'demand',        'CREATE',  2,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(3,  'demand:update',  '编辑补货需求', '補充需要を編集', 'demand',        'UPDATE',  3,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(4,  'demand:delete',  '删除补货需求', '補充需要を削除', 'demand',        'DELETE',  4,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- procurement CRUD
(5,  'procurement:read',    '查看发注单',   '発注書を表示', 'procurement', 'READ',    5,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(6,  'procurement:create',  '创建发注单',   '発注書を作成', 'procurement', 'CREATE',  6,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(7,  'procurement:update',  '编辑发注单',   '発注書を編集', 'procurement', 'UPDATE',  7,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(8,  'procurement:delete',  '删除发注单',   '発注書を削除', 'procurement', 'DELETE',  8,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- shipment CRUD
(9,  'shipment:read',   '查看出货批次', '出荷バッチを表示', 'shipment', 'READ',    9,  1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(10, 'shipment:create', '创建出货批次', '出荷バッチを作成', 'shipment', 'CREATE',  10, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(11, 'shipment:update', '编辑出货批次', '出荷バッチを編集', 'shipment', 'UPDATE',  11, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(12, 'shipment:delete', '删除出货批次', '出荷バッチを削除', 'shipment', 'DELETE',  12, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- qc CRUD
(13, 'qc:read',    '查看验货记录', '検品記録を表示', 'qc', 'READ',    13, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(14, 'qc:create',  '创建验货记录', '検品記録を作成', 'qc', 'CREATE',  14, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(15, 'qc:update',  '编辑验货记录', '検品記録を編集', 'qc', 'UPDATE',  15, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(16, 'qc:delete',  '删除验货记录', '検品記録を削除', 'qc', 'DELETE',  16, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- logistics CRUD
(21, 'logistics:read',    '查看物流调配', '物流配送を表示', 'logistics',     'READ',    21, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(22, 'logistics:create',  '创建物流调配', '物流配送を作成', 'logistics',     'CREATE',  22, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(23, 'logistics:update',  '编辑物流调配', '物流配送を編集', 'logistics',     'UPDATE',  23, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(24, 'logistics:delete',  '删除物流调配', '物流配送を削除', 'logistics',     'DELETE',  24, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- consolidation CRUD
(25, 'consolidation:read',    '查看拼柜池', 'コンソリを表示', 'consolidation', 'READ',    25, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(26, 'consolidation:create',  '创建拼柜池', 'コンソリを作成', 'consolidation', 'CREATE',  26, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(27, 'consolidation:update',  '编辑拼柜池', 'コンソリを編集', 'consolidation', 'UPDATE',  27, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(28, 'consolidation:delete',  '删除拼柜池', 'コンソリを削除', 'consolidation', 'DELETE',  28, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- container CRUD
(29, 'container:read',    '查看货柜', 'コンテナを表示', 'container', 'READ',    29, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(30, 'container:create',  '创建货柜', 'コンテナを作成', 'container', 'CREATE',  30, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(31, 'container:update',  '编辑货柜', 'コンテナを編集', 'container', 'UPDATE',  31, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(32, 'container:delete',  '删除货柜', 'コンテナを削除', 'container', 'DELETE',  32, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- customs CRUD + approve
(41, 'customs:read',    '查看国内报关', '国内通関を表示', 'customs', 'READ',    41, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(42, 'customs:create',  '创建报关单',   '通関書類を作成', 'customs', 'CREATE',  42, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(43, 'customs:update',  '编辑报关单',   '通関書類を編集', 'customs', 'UPDATE',  43, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(44, 'customs:delete',  '删除报关单',   '通関書類を削除', 'customs', 'DELETE',  44, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(45, 'customs:approve', '审批报关单',   '通関書類を承認', 'customs', 'APPROVE', 45, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- japan_customs CRUD + start + complete
(46, 'japan_customs:read',    '查看日本清关', '日本通関を表示', 'japan_customs', 'READ',    46, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(47, 'japan_customs:create',  '创建日本清关', '日本通関を作成', 'japan_customs', 'CREATE',  47, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(48, 'japan_customs:start',   '启动清关',   '通関を開始', 'japan_customs', 'START',   48, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(49, 'japan_customs:complete','完成清关',  '通関を完了', 'japan_customs', 'COMPLETE',49, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(50, 'japan_customs:delete', '删除日本清关', '日本通関を削除', 'japan_customs', 'DELETE',  50, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- tax_refund CRUD + complete
(61, 'tax_refund:read',    '查看退税记录', '退税記録を表示', 'tax_refund', 'READ',    61, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(62, 'tax_refund:create',  '创建退税记录', '退税記録を作成', 'tax_refund', 'CREATE',  62, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(63, 'tax_refund:update',  '编辑退税记录', '退税記録を編集', 'tax_refund', 'UPDATE',  63, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(64, 'tax_refund:complete', '完成退税',   '退税を完了', 'tax_refund', 'COMPLETE',64, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(65, 'tax_refund:delete',  '删除退税记录', '退税記録を削除', 'tax_refund', 'DELETE',  65, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- sales CRUD
(66, 'sales:read',    '查看销售记录', '販売記録を表示', 'sales', 'READ',    66, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(67, 'sales:create',  '创建销售记录', '販売記録を作成', 'sales', 'CREATE',  67, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(68, 'sales:update',  '编辑销售记录', '販売記録を編集', 'sales', 'UPDATE',  68, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(69, 'sales:delete',  '删除销售记录', '販売記録を削除', 'sales', 'DELETE',  69, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- factory CRUD
(70, 'factory:read',    '查看工厂', '工場を表示', 'factory', 'READ',    70, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(71, 'factory:create',  '创建工厂', '工場を作成', 'factory', 'CREATE',  71, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(72, 'factory:update',  '编辑工厂', '工場を編集', 'factory', 'UPDATE',  72, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(73, 'factory:delete',  '删除工厂', '工場を削除', 'factory', 'DELETE',  73, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- product CRUD
(74, 'product:read',    '查看商品', '商品を表示', 'product', 'READ',    74, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(75, 'product:create',  '创建商品', '商品を作成', 'product', 'CREATE',  75, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(76, 'product:update',  '编辑商品', '商品を編集', 'product', 'UPDATE',  76, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(77, 'product:delete',  '删除商品', '商品を削除', 'product', 'DELETE',  77, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- order read
(78, 'order:read', '查看订单总览', '注文一覧を表示', 'order', 'READ', 78, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- user CRUD + admin + approve + reset
(81, 'user:read',          '查看用户',     'ユーザーを表示', 'user', 'READ',    81, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(82, 'user:create',        '创建用户',     'ユーザーを作成', 'user', 'CREATE',  82, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(83, 'user:update',        '编辑用户',     'ユーザーを編集', 'user', 'UPDATE',  83, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(84, 'user:delete',        '删除用户',     'ユーザーを削除', 'user', 'DELETE',  84, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(85, 'user:approve',       '审核注册用户', 'ユーザー登録を承認', 'user', 'APPROVE', 85, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(86, 'user:reset_password', '重置用户密码', 'パスワードをリセット', 'user', 'ADMIN',   86, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- role CRUD + assign + delete
(87, 'role:read',    '查看角色',     '役割を表示', 'role', 'READ',    87, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(88, 'role:create',  '创建角色',     '役割を作成', 'role', 'CREATE',  88, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(89, 'role:update',  '编辑角色',     '役割を編集', 'role', 'UPDATE',  89, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(90, 'role:assign',  '分配角色',     '役割を割り当て', 'role', 'ASSIGN',  90, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(91, 'role:delete',  '删除角色',     '役割を削除', 'role', 'DELETE',  91, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- permission read
(92, 'permission:read', '查看权限', '権限を表示', 'permission', 'READ', 92, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- audit read + export
(93, 'audit:read',    '查看操作日志', '操作ログを表示', 'audit', 'READ',    93, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(94, 'audit:export',  '导出操作日志', '操作ログをエクスポート', 'audit', 'EXPORT',  94, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- warehouse CRUD (V15 新增)
(101, 'warehouse:read',    '查看仓储记录', '倉庫記録を表示', 'warehouse',    'READ',   101, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(102, 'warehouse:create',  '创建仓储记录', '倉庫記録を作成', 'warehouse',    'CREATE', 102, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(103, 'warehouse:update',  '编辑仓储记录', '倉庫記録を編集', 'warehouse',    'UPDATE', 103, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(104, 'warehouse:delete',  '删除仓储记录', '倉庫記録を削除', 'warehouse',    'DELETE', 104, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
-- notification CRUD (V15 新增)
(111, 'notification:read',    '查看通知', '通知を表示', 'notification', 'READ',   111, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(112, 'notification:create',  '创建通知', '通知を作成', 'notification', 'CREATE', 112, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(113, 'notification:update',  '编辑通知', '通知を編集', 'notification', 'UPDATE', 113, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
(114, 'notification:delete',  '删除通知', '通知を削除', 'notification', 'DELETE', 114, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');

-- 管理员用户（admin/admin123，BCrypt strength=12）
INSERT IGNORE INTO user (id, user_code, username, password_hash, name_cn, name_jp, email, phone, company_id, department_id, status, registration_status, language, timezone, create_by, create_time, update_time, is_deleted)
VALUES (1, 'U-0001', 'admin', '$2a$12$t7mRpfsCDNFgj6LET1Y47eH7J2.MJ5i5nAYwYL6SfKdWE7LN.vqUG',
    '系统管理员', 'システム管理者', 'admin@manpou.cn', '+86-138-0000-0001',
    1, 1, 1, 'APPROVED', 'zh', 'Asia/Shanghai', 'SYSTEM', NOW(3), NOW(3), 0);

-- admin → ADMIN 角色关联
INSERT IGNORE INTO user_role (id, user_id, role_id, create_time)
VALUES (1, 1, 1, NOW(3));

-- admin → 经理职务
INSERT IGNORE INTO user_position (id, user_id, position_id, create_time)
VALUES (1, 1, 4, NOW(3));

-- 角色权限分配（全部使用 INSERT IGNORE，幂等）
-- ADMIN (role_id=1): 全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE is_deleted = 0;

-- MANAGER (role_id=2): 全部业务权限（不含系统管理写操作）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code NOT IN (
        'user:delete', 'user:approve', 'user:reset_password',
        'role:create', 'role:update', 'role:assign', 'role:delete',
        'permission:read'
  );

-- OPERATOR (role_id=3): 业务 CRUD + order:read
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 3, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN (
        'demand', 'procurement', 'shipment', 'qc',
        'logistics', 'consolidation', 'container',
        'customs', 'japan_customs',
        'tax_refund', 'sales', 'factory', 'product', 'order'
      )
  AND p.action_ IN ('READ', 'CREATE', 'UPDATE', 'DELETE');

-- VIEWER (role_id=4): 业务只读
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 4, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN (
        'demand', 'procurement', 'shipment', 'qc',
        'logistics', 'consolidation', 'container',
        'customs', 'japan_customs',
        'tax_refund', 'sales', 'factory', 'product', 'order'
      )
  AND p.action_ = 'READ';
