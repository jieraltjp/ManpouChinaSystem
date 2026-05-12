package com.manpou.allinone.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V38 一次性迁移：将 replenishment_demand.status 从 ENUM 改为 VARCHAR(32)。
 * Hibernate ddl-auto=update 会自动执行 schema 变更，此组件用于幂等兜底。
 */
@Component
public class DemandStatusMigrationConfig {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        String colType = getColumnType();
        if (colType == null) {
            // 表尚未就绪，忽略
            return;
        }
        if ("varchar(32)".equalsIgnoreCase(colType) || "VARCHAR(32)".equals(colType)) {
            System.out.println("[DemandStatusMigration] status 已是 VARCHAR(32)，跳过");
            return;
        }
        System.out.println("[DemandStatusMigration] 将 status 从 " + colType + " 改为 VARCHAR(32)");
        jdbc.execute("ALTER TABLE replenishment_demand " +
            "MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
        System.out.println("[DemandStatusMigration] 完成");
    }

    private String getColumnType() {
        try {
            return jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'REPLENISHMENT_DEMAND' AND COLUMN_NAME = 'STATUS'",
                String.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
