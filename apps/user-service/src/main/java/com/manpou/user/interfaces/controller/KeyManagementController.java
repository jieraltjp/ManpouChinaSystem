package com.manpou.user.interfaces.controller;

import com.manpou.user.application.KeyManagementService;
import com.manpou.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密钥管理控制器（Admin 专用）。
 *
 * 安全说明：此接口必须仅对管理员开放（通过 RBAC 权限控制）。
 * 详见 docs/pro/02-user-service.md §认证授权
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/keys")
@RequiredArgsConstructor
@Tag(name = "密钥管理", description = "RS256 签名密钥轮换（Admin 专用）")
@SecurityRequirement(name = "bearerAuth")
public class KeyManagementController {

    private final KeyManagementService keyManagementService;

    /**
     * 轮换密钥（已禁用）。
     * 当前使用环境变量加载密钥，轮换请更新 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY 环境变量后重启。
     */
    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "轮换签名密钥（已禁用）")
    public Result<String> rotateKey() {
        return Result.fail("auth.key-rotation-disabled",
            "密钥轮换已禁用：使用环境变量 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY 管理密钥，更新后重启生效");
    }

    /**
     * 查询所有密钥元数据（已禁用）。
     * 当前使用环境变量加载密钥，无 DB 元数据。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询所有密钥（已禁用）")
    public Result<List<KeyManagementService.KeyInfo>> listKeys() {
        return Result.fail("auth.key-list-disabled",
            "密钥列表已禁用：使用环境变量 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY 管理密钥");
    }
}
