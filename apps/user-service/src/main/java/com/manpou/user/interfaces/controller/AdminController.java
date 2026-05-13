package com.manpou.user.interfaces.controller;

import com.manpou.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部管理接口（仅 Admin）。
 * <p>临时用于修复 user.avatar_url 列类型（VARCHAR(512) → MEDIUMTEXT）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/db")
@RequiredArgsConstructor
@Tag(name = "数据库管理", description = "内部 DB 操作（Admin 专用）")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 修复 user.avatar_url 列类型。
     * V17 迁移已在 allinone JAR 中，但 user_service DB（实际连接 manpou）的列未变更。
     * 此接口用于一次性修复，修复后不再需要。
     */
    @PostMapping("/migrate-avatar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "迁移 avatar_url 列为 MEDIUMTEXT（一次性）")
    public Result<String> migrateAvatarColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE `user` MODIFY COLUMN `avatar_url` MEDIUMTEXT DEFAULT NULL COMMENT '头像Base64或URL'");
            log.info("[DB Migrate] avatar_url column changed to MEDIUMTEXT");
            return Result.ok("avatar_url column migrated to MEDIUMTEXT");
        } catch (Exception ex) {
            log.error("[DB Migrate] failed: {}", ex.getMessage());
            return Result.fail("db.migration-failed", ex.getMessage());
        }
    }
}
