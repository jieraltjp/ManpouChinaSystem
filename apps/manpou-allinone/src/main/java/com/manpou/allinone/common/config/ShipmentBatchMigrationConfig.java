package com.manpou.allinone.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V40-V43 一次性迁移（SPEC-B11 §11）。
 * Flyway 被禁用（使用 Hibernate ddl-auto），此组件手工执行 DDL。
 * 幂等：已执行时跳过。
 */
@Component
public class ShipmentBatchMigrationConfig {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        migrateV40();
        migrateV41();
        migrateV42();
        migrateV43();
    }

    private void migrateV40() {
        if (tableExists("shipment_batch")) {
            System.out.println("[V40] shipment_batch 表已存在，跳过");
            return;
        }
        jdbc.execute("""
            CREATE TABLE shipment_batch (
                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                procurement_id       BIGINT NOT NULL,
                batch_code           VARCHAR(32) NOT NULL,
                shipment_quantity    INT NOT NULL,
                factory_ship_date    DATE,
                actual_ship_date     DATE,
                status               VARCHAR(16) NOT NULL DEFAULT '待验货',
                remarks              VARCHAR(512),
                create_time          DATETIME(3) NOT NULL,
                update_time          DATETIME(3) NOT NULL,
                create_by            VARCHAR(64) NOT NULL,
                update_by            VARCHAR(64) NOT NULL,
                is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
                INDEX idx_sb_procurement (procurement_id),
                INDEX idx_sb_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        System.out.println("[V40] shipment_batch 表创建完成");
    }

    private void migrateV41() {
        if (tableExists("demand_procurement_mapping")) {
            System.out.println("[V41] demand_procurement_mapping 表已存在，跳过");
            return;
        }
        jdbc.execute("""
            CREATE TABLE demand_procurement_mapping (
                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                demand_id            BIGINT NOT NULL,
                procurement_id       BIGINT NOT NULL,
                allocated_quantity   INT NOT NULL,
                status               VARCHAR(16) NOT NULL DEFAULT '进行中',
                create_time          DATETIME(3) NOT NULL,
                update_time          DATETIME(3) NOT NULL,
                create_by            VARCHAR(64) NOT NULL,
                update_by            VARCHAR(64) NOT NULL,
                is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
                UNIQUE KEY uk_demand_procurement (demand_id, procurement_id),
                INDEX idx_mapping_demand (demand_id),
                INDEX idx_mapping_procurement (procurement_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        System.out.println("[V41] demand_procurement_mapping 表创建完成");
    }

    private void migrateV42() {
        // V42：回填存量 linked_procurement_id → demand_procurement_mapping
        Long count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM replenishment_demand
            WHERE linked_procurement_id IS NOT NULL
              AND deleted = false
              AND NOT EXISTS (
                  SELECT 1 FROM demand_procurement_mapping m
                  WHERE m.demand_id = replenishment_demand.id
                    AND m.procurement_id = replenishment_demand.linked_procurement_id
                    AND m.is_deleted = false
              )
            """, Long.class);
        if (count == null || count == 0) {
            System.out.println("[V42] 无需回填数据，跳过");
            return;
        }
        jdbc.update("""
            INSERT INTO demand_procurement_mapping
                (demand_id, procurement_id, allocated_quantity, status,
                 create_time, update_time, create_by, update_by, is_deleted)
            SELECT
                d.id,
                d.linked_procurement_id,
                d.quantity,
                '进行中',
                NOW(3), NOW(3), 'SYSTEM', 'SYSTEM', 0
            FROM replenishment_demand d
            WHERE d.linked_procurement_id IS NOT NULL
              AND d.deleted = false
              AND NOT EXISTS (
                  SELECT 1 FROM demand_procurement_mapping m
                  WHERE m.demand_id = d.id
                    AND m.procurement_id = d.linked_procurement_id
                    AND m.is_deleted = false
              )
            """);
        System.out.println("[V42] 回填 " + count + " 条映射数据完成");
    }

    private void migrateV43() {
        // 检查 shipment_batch_id 列是否已存在
        if (columnExists("qc_record", "shipment_batch_id")) {
            System.out.println("[V43] qc_record.shipment_batch_id 列已存在，跳过");
            return;
        }
        jdbc.execute("ALTER TABLE qc_record ADD COLUMN shipment_batch_id BIGINT NULL COMMENT '关联出货批次ID'");
        jdbc.execute("ALTER TABLE qc_record ADD INDEX idx_qc_shipment_batch (shipment_batch_id)");
        System.out.println("[V43] qc_record.shipment_batch_id 列添加完成");

        // 回填存量 QcRecord → ShipmentBatch（历史数据批次）
        Long batchCount = jdbc.queryForObject("""
            SELECT COUNT(DISTINCT procurement_id) FROM qc_record
            WHERE deleted = false AND procurement_id IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM shipment_batch sb
                  WHERE sb.procurement_id = qc_record.procurement_id
                    AND sb.remarks LIKE '%历史数据迁移%'
              )
            """, Long.class);
        if (batchCount != null && batchCount > 0) {
            jdbc.update("""
                INSERT INTO shipment_batch
                    (procurement_id, batch_code, shipment_quantity, factory_ship_date, actual_ship_date,
                     status, remarks, create_time, update_time, create_by, update_by, is_deleted)
                SELECT
                    qc.procurement_id,
                    CONCAT('SB-HISTORY-', qc.procurement_id, '-', MIN(qc.id)),
                    COALESCE(SUM(qc.inspection_count), 0),
                    MIN(qc.qc_date),
                    MIN(qc.qc_date),
                    '已验货',
                    '历史数据迁移（V43）',
                    NOW(3), NOW(3), 'SYSTEM', 'SYSTEM', 0
                FROM qc_record qc
                WHERE qc.deleted = false
                  AND qc.procurement_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM shipment_batch sb
                      WHERE sb.procurement_id = qc.procurement_id
                        AND sb.remarks LIKE '%历史数据迁移%'
                  )
                GROUP BY qc.procurement_id
                """);
            System.out.println("[V43] 回填 " + batchCount + " 个历史 ShipmentBatch 完成");
        }

        // 关联 QcRecord → ShipmentBatch
        int linkedCount = jdbc.queryForObject("""
            SELECT COUNT(*) FROM qc_record qc
            JOIN (
                SELECT MIN(id) AS batch_id, procurement_id
                FROM shipment_batch
                WHERE remarks LIKE '%历史数据迁移%'
                GROUP BY procurement_id
            ) sb ON sb.procurement_id = qc.procurement_id
            WHERE qc.deleted = false
              AND qc.shipment_batch_id IS NULL
              AND qc.procurement_id IS NOT NULL
            """, Integer.class);
        if (linkedCount > 0) {
            jdbc.update("""
                UPDATE qc_record qc
                JOIN (
                    SELECT MIN(id) AS batch_id, procurement_id
                    FROM shipment_batch
                    WHERE remarks LIKE '%历史数据迁移%'
                    GROUP BY procurement_id
                ) sb ON sb.procurement_id = qc.procurement_id
                SET qc.shipment_batch_id = sb.batch_id
                WHERE qc.deleted = false
                  AND qc.shipment_batch_id IS NULL
                  AND qc.procurement_id IS NOT NULL
                """);
            System.out.println("[V43] 关联 " + linkedCount + " 条 QcRecord 完成");
        }

        // procurement_id 改为 nullable
        String colType = jdbc.queryForObject(
            "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qc_record' AND COLUMN_NAME = 'procurement_id'",
            String.class);
        if (colType != null && colType.contains("NOT NULL")) {
            jdbc.execute("ALTER TABLE qc_record MODIFY COLUMN procurement_id BIGINT NULL");
            System.out.println("[V43] qc_record.procurement_id 改为 nullable 完成");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
            Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
