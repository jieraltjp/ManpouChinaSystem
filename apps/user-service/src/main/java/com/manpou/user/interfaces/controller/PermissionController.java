package com.manpou.user.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.user.application.dto.PermissionTreeVO;
import com.manpou.user.application.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理接口。
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RoleService roleService;

    /**
     * 权限树（按模块分组）。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('permission:read')")
    public Result<List<PermissionTreeVO>> getTree() {
        return Result.ok(roleService.getPermissionTree());
    }
}
