package com.manpou.user;

import com.alibaba.cloud.nacos.NacosConfigAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务启动入口。
 */
@SpringBootApplication(
    exclude = {
        NacosConfigAutoConfiguration.class,
        NacosDiscoveryAutoConfiguration.class,
    }
)
@EnableAsync
@RequiredArgsConstructor
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class DatabaseInitializer {
        private final JdbcTemplate jdbc;

        @EventListener(ApplicationReadyEvent.class)
        public void init() {
            log.info("Checking user table columns...");
            addColumnIfNotExists("language", "VARCHAR(10) DEFAULT 'zh' COMMENT '界面语言'");
            addColumnIfNotExists("timezone", "VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '界面时区'");
            alterColumnIfTooSmall("avatar_url", "MEDIUMTEXT DEFAULT NULL COMMENT '头像Base64或URL'");
            log.info("User table columns check done.");
        }

        private void addColumnIfNotExists(String column, String definition) {
            try {
                jdbc.execute("ALTER TABLE `user` ADD COLUMN " + column + " " + definition);
                log.info("Added column: {}", column);
            } catch (Exception e) {
                log.debug("Column {} already exists or error: {}", column, e.getMessage());
            }
        }

        private void alterColumnIfTooSmall(String column, String newDefinition) {
            try {
                jdbc.execute("ALTER TABLE `user` MODIFY COLUMN " + column + " " + newDefinition);
                log.info("Modified column {} to {}", column, newDefinition.split(" ")[0]);
            } catch (Exception e) {
                log.debug("Column {} modify skipped or error: {}", column, e.getMessage());
            }
        }
    }
}
