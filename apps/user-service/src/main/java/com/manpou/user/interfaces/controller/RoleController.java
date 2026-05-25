package com.manpou.user.interfaces.controller;

import com.manpou.common.annotation.AuditLog;
import com.manpou.common.result.Result;
import com.manpou.user.application.dto.*;
import com.manpou.user.application.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public Result<List<RoleSimpleVO>> listAll() {
        return Result.ok(roleService.listAll());
    }

    /**
     * 角色详情（含权限列表）。
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('role:read')")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    /**
     * 新建角色。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    @AuditLog(module = "role", action = "CREATE", resourceType = "role")
    public Result<RoleVO> create(@Valid @RequestBody RoleCreateCmd cmd) {
        return Result.ok(roleService.create(cmd));
    }

    /**
     * 更新角色。
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('role:update')")
    @AuditLog(module = "role", action = "UPDATE", resourceType = "role", resourceId = "#id")
    public Result<RoleVO> update(@PathVariable Long id,
                                  @RequestBody RoleUpdateCmd cmd) {
        return Result.ok(roleService.update(id, cmd));
    }

    /**
     * 删除角色。
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('role:delete')")
    @AuditLog(module = "role", action = "DELETE", resourceType = "role", resourceId = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    /**
     * 分配权限。
     */
    @PutMapping("/{id:\\d+}/permissions")
    @PreAuthorize("hasAuthority('role:assign')")
    @AuditLog(module = "role", action = "ASSIGN_PERMISSIONS", resourceType = "role_permission", resourceId = "#id")
    public Result<RoleVO> assignPermissions(@PathVariable Long id,
                                           @RequestBody RolePermissionsCmd cmd) {
        return Result.ok(roleService.assignPermissions(id, cmd));
    }

    /**
     * 更新角色属性（仅 isEditable / description）。
     */
    @PatchMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "role", action = "PATCH", resourceType = "role", resourceId = "#id")
    public Result<RoleVO> patch(@PathVariable Long id,
                                @RequestBody RolePatchCmd cmd) {
        return Result.ok(roleService.patch(id, cmd));
    }
}
