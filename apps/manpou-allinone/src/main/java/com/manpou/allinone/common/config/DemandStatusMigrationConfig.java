package com.manpou.allinone.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V38 一次性迁移：将 replenishment_demand.status 从 ENUM 改为 VARCHAR(32)。
 * Flyway 被禁用（使用 Hibernate ddl-auto），此组件手工执行 DDL。
 * 此迁移幂等：已执行时 CHECKSUM 校验会跳过。
 */
@Component
public class DemandStatusMigrationConfig {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        String colType = jdbc.queryForObject(
            "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'replenishment_demand' AND COLUMN_NAME = 'status'",
            String.class);

        if ("varchar(32)".equalsIgnoreCase(colType)) {
            System.out.println("[DemandStatusMigration] status 已是 VARCHAR(32)，跳过");
            return;
        }

        System.out.println("[DemandStatusMigration] 将 status 从 " + colType + " 改为 VARCHAR(32)");
        jdbc.execute("ALTER TABLE replenishment_demand " +
            "MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' " +
            "COMMENT '状态：PENDING=待确认，CONFIRMED=已确认，CONVERTED=已转采购，CANCELLED=已取消'");
        System.out.println("[DemandStatusMigration] 完成");
    }
}
