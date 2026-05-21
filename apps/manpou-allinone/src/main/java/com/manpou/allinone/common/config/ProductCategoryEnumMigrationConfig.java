package com.manpou.allinone.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V21 一次性迁移：将 product.category ENUM 从 3 项扩展为 8 项。
 * Flyway 在开发环境禁用（flyway.enabled: false），改用此幂等组件兜底。
 */
@Component
public class ProductCategoryEnumMigrationConfig {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        String colType = getColumnType();
        if (colType == null) {
            System.out.println("[ProductCategoryMigration] product 表不存在，跳过");
            return;
        }
        if (colType.toUpperCase().contains("INDEPENDENT")) {
            System.out.println("[ProductCategoryMigration] category 已是 8 项 ENUM，跳过");
            return;
        }
        System.out.println("[ProductCategoryMigration] 将 category 从 " + colType + " 扩展为 8 项 ENUM");
        jdbc.execute("ALTER TABLE product " +
            "MODIFY COLUMN `category` enum('OEM','ORDINARY','FACTORY_DIRECT','NORMAL','SAMPLE','SELF_USE','PARTS','INDEPENDENT') DEFAULT NULL");
        System.out.println("[ProductCategoryMigration] 完成");
    }

    private String getColumnType() {
        try {
            return jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'category'",
                String.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
