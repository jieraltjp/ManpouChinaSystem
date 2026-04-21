package com.manpou.logistics.interfaces.controller;

import com.manpou.logistics.application.KeyManagementService;
import com.manpou.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密钥管理控制器（Admin 专用）。
 *
 * 安全说明：此接口必须仅对管理员开放（通过 RBAC 权限控制）。
 * 详见 docs/pro/00-root-project.md §2.2
 */
@RestController
@RequestMapping("/api/v1/admin/keys")
@RequiredArgsConstructor
@Tag(name = "密钥管理", description = "RS256 签名密钥轮换（Admin 专用）")
@SecurityRequirement(name = "bearerAuth")
public class KeyManagementController {

    private final KeyManagementService keyManagementService;

    /**
     * 轮换密钥：停用当前密钥，生成并激活新密钥（原子操作）。
     */
    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "轮换签名密钥",
        description = "停用当前 ACTIVE 密钥，生成并激活新密钥。旧密钥保留用于验签历史 Token。"
    )
    public Result<String> rotateKey() {
        String newKid = keyManagementService.rotateKey();
        return Result.ok(newKid);
    }

    /**
     * 查询所有密钥元数据（不含私钥）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询所有密钥", description = "列出所有密钥的元数据（不含私钥内容）。")
    public Result<List<KeyManagementService.KeyInfo>> listKeys() {
        return Result.ok(keyManagementService.listKeys());
    }
}
