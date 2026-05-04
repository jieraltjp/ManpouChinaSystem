-- V8: 角色表
CREATE TABLE role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code       VARCHAR(32)  NOT NULL UNIQUE,
    role_name_cn    VARCHAR(64)  NOT NULL,
    role_name_jp    VARCHAR(64)  NOT NULL,
    role_type       VARCHAR(16)  COMMENT 'SYSTEM/BUSINESS',
    description     VARCHAR(256),
    is_editable     TINYINT      DEFAULT 1 COMMENT '0=系统内置不可编辑',
    status          TINYINT      DEFAULT 1,
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted      TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置角色（is_editable=1，后续可通过 API 修改）
INSERT INTO role (role_code, role_name_cn, role_name_jp, role_type, is_editable, create_time, update_time) VALUES
('ADMIN',   '系统管理员',   'システム管理者',    'SYSTEM',   1, NOW(3), NOW(3)),
('MANAGER', '运营主管',     '運営マネージャー', 'BUSINESS', 1, NOW(3), NOW(3)),
('OPERATOR','普通运营',     '一般運営者',       'BUSINESS', 1, NOW(3), NOW(3)),
('VIEWER',  '查看者',       '閲覧者',           'BUSINESS', 1, NOW(3), NOW(3));

-- 权限表（72 条）
CREATE TABLE permission (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code     VARCHAR(64)  NOT NULL UNIQUE,
    permission_name_cn  VARCHAR(64)  NOT NULL,
    permission_name_jp  VARCHAR(64)  NOT NULL,
    module              VARCHAR(32)  NOT NULL,
    action_             VARCHAR(16)  NOT NULL COMMENT 'READ/CREATE/UPDATE/DELETE/APPROVE/START/COMPLETE/EXPORT',
    description          VARCHAR(256),
    sort_order          INT          DEFAULT 0,
    status              TINYINT      DEFAULT 1,
    create_time         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    is_deleted          TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 发注管理模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('demand:read', '查看补货需求', '補充需要を表示', 'demand', 'READ', 1),
('demand:create', '创建补货需求', '補充需要を作成', 'demand', 'CREATE', 2),
('demand:update', '编辑补货需求', '補充需要を編集', 'demand', 'UPDATE', 3),
('demand:delete', '删除补货需求', '補充需要を削除', 'demand', 'DELETE', 4),
('procurement:read', '查看发注单', '発注書を表示', 'procurement', 'READ', 5),
('procurement:create', '创建发注单', '発注書を作成', 'procurement', 'CREATE', 6),
('procurement:update', '编辑发注单', '発注書を編集', 'procurement', 'UPDATE', 7),
('procurement:delete', '删除发注单', '発注書を削除', 'procurement', 'DELETE', 8),
('shipment:read', '查看出货批次', '出荷バッチを表示', 'shipment', 'READ', 9),
('shipment:create', '创建出货批次', '出荷バッチを作成', 'shipment', 'CREATE', 10),
('shipment:update', '编辑出货批次', '出荷バッチを編集', 'shipment', 'UPDATE', 11),
('shipment:delete', '删除出货批次', '出荷バッチを削除', 'shipment', 'DELETE', 12),
('qc:read', '查看验货记录', '検品記録を表示', 'qc', 'READ', 13),
('qc:create', '创建验货记录', '検品記録を作成', 'qc', 'CREATE', 14),
('qc:update', '编辑验货记录', '検品記録を編集', 'qc', 'UPDATE', 15),
('qc:delete', '删除验货记录', '検品記録を削除', 'qc', 'DELETE', 16);

-- 物流模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('logistics:read', '查看物流调配', '物流配送を表示', 'logistics', 'READ', 21),
('logistics:create', '创建物流调配', '物流配送を作成', 'logistics', 'CREATE', 22),
('logistics:update', '编辑物流调配', '物流配送を編集', 'logistics', 'UPDATE', 23),
('logistics:delete', '删除物流调配', '物流配送を削除', 'logistics', 'DELETE', 24),
('consolidation:read', '查看拼柜池', 'コンソリを表示', 'consolidation', 'READ', 25),
('consolidation:create', '创建拼柜池', 'コンソリを作成', 'consolidation', 'CREATE', 26),
('consolidation:update', '编辑拼柜池', 'コンソリを編集', 'consolidation', 'UPDATE', 27),
('consolidation:delete', '删除拼柜池', 'コンソリを削除', 'consolidation', 'DELETE', 28),
('container:read', '查看货柜', 'コンテナを表示', 'container', 'READ', 29),
('container:create', '创建货柜', 'コンテナを作成', 'container', 'CREATE', 30),
('container:update', '编辑货柜', 'コンテナを編集', 'container', 'UPDATE', 31),
('container:delete', '删除货柜', 'コンテナを削除', 'container', 'DELETE', 32);

-- 报关模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('customs:read', '查看国内报关', '国内通関を表示', 'customs', 'READ', 41),
('customs:create', '创建报关单', '通関書類を作成', 'customs', 'CREATE', 42),
('customs:update', '编辑报关单', '通関書類を編集', 'customs', 'UPDATE', 43),
('customs:delete', '删除报关单', '通関書類を削除', 'customs', 'DELETE', 44),
('customs:approve', '审批报关单', '通関書類を承認', 'customs', 'APPROVE', 45),
('japan_customs:read', '查看日本清关', '日本通関を表示', 'japan_customs', 'READ', 46),
('japan_customs:create', '创建日本清关', '日本通関を作成', 'japan_customs', 'CREATE', 47),
('japan_customs:start', '启动清关', '通関を開始', 'japan_customs', 'START', 48),
('japan_customs:complete', '完成清关', '通関を完了', 'japan_customs', 'COMPLETE', 49),
('japan_customs:delete', '删除日本清关', '日本通関を削除', 'japan_customs', 'DELETE', 50);

-- 财务/销售/基础模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('tax_refund:read', '查看退税记录', '退税記録を表示', 'tax_refund', 'READ', 61),
('tax_refund:create', '创建退税记录', '退税記録を作成', 'tax_refund', 'CREATE', 62),
('tax_refund:update', '编辑退税记录', '退税記録を編集', 'tax_refund', 'UPDATE', 63),
('tax_refund:complete', '完成退税', '退税を完了', 'tax_refund', 'COMPLETE', 64),
('tax_refund:delete', '删除退税记录', '退税記録を削除', 'tax_refund', 'DELETE', 65),
('sales:read', '查看销售记录', '販売記録を表示', 'sales', 'READ', 66),
('sales:create', '创建销售记录', '販売記録を作成', 'sales', 'CREATE', 67),
('sales:update', '编辑销售记录', '販売記録を編集', 'sales', 'UPDATE', 68),
('sales:delete', '删除销售记录', '販売記録を削除', 'sales', 'DELETE', 69),
('factory:read', '查看工厂', '工場を表示', 'factory', 'READ', 70),
('factory:create', '创建工厂', '工場を作成', 'factory', 'CREATE', 71),
('factory:update', '编辑工厂', '工場を編集', 'factory', 'UPDATE', 72),
('factory:delete', '删除工厂', '工場を削除', 'factory', 'DELETE', 73),
('product:read', '查看商品', '商品を表示', 'product', 'READ', 74),
('product:create', '创建商品', '商品を作成', 'product', 'CREATE', 75),
('product:update', '编辑商品', '商品を編集', 'product', 'UPDATE', 76),
('product:delete', '删除商品', '商品を削除', 'product', 'DELETE', 77),
('order:read', '查看订单总览', '注文一覧を表示', 'order', 'READ', 78);

-- 系统管理模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('user:read', '查看用户', 'ユーザーを表示', 'user', 'READ', 81),
('user:create', '创建用户', 'ユーザーを作成', 'user', 'CREATE', 82),
('user:update', '编辑用户', 'ユーザーを編集', 'user', 'UPDATE', 83),
('user:delete', '删除用户', 'ユーザーを削除', 'user', 'DELETE', 84),
('user:approve', '审核注册用户', 'ユーザー登録を承認', 'user', 'APPROVE', 85),
('user:reset_password', '重置用户密码', 'パスワードをリセット', 'user', 'ADMIN', 86),
('role:read', '查看角色', '役割を表示', 'role', 'READ', 87),
('role:create', '创建角色', '役割を作成', 'role', 'CREATE', 88),
('role:update', '编辑角色', '役割を編集', 'role', 'UPDATE', 89),
('role:assign', '分配角色', '役割を割り当て', 'role', 'ASSIGN', 90),
('permission:read', '查看权限', '権限を表示', 'permission', 'READ', 91),
('audit:read', '查看操作日志', '操作ログを表示', 'audit', 'READ', 92),
('audit:export', '导出操作日志', '操作ログをエクスポート', 'audit', 'EXPORT', 93);

-- 用户角色关联表
CREATE TABLE user_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    role_id     BIGINT       NOT NULL,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- admin → ADMIN 角色
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);

-- 角色权限关联表
CREATE TABLE role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT       NOT NULL,
    permission_id BIGINT       NOT NULL,
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ADMIN 拥有所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE is_deleted = 0;
