package com.manpou.user.interfaces.controller;

import com.manpou.common.annotation.AuditLog;
import com.manpou.common.result.Result;
import com.manpou.user.application.dto.*;
import com.manpou.user.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public Result<UserPageVO> pageQuery(UserPageQuery query) {
        return Result.ok(userService.pageQuery(query));
    }

    /**
     * 获取用户详情。
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * 新建用户。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    @AuditLog(module = "user", action = "CREATE", resourceType = "user")
    public Result<UserVO> create(@Valid @RequestBody UserCreateCmd cmd) {
        return Result.ok(userService.create(cmd));
    }

    /**
     * 更新用户。
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('user:update')")
    @AuditLog(module = "user", action = "UPDATE", resourceType = "user", resourceId = "#id")
    public Result<UserVO> update(@PathVariable Long id,
                                  @RequestBody UserUpdateCmd cmd) {
        return Result.ok(userService.update(id, cmd));
    }

    /**
     * 删除用户（软删除）。
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('user:delete')")
    @AuditLog(module = "user", action = "DELETE", resourceType = "user", resourceId = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /**
     * 启用/禁用用户。
     */
    @PutMapping("/{id:\\d+}/status")
    @PreAuthorize("hasAuthority('user:update')")
    @AuditLog(module = "user", action = "UPDATE_STATUS", resourceType = "user", resourceId = "#id")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody UserStatusCmd cmd) {
        userService.updateStatus(id, cmd.getStatus());
        return Result.ok();
    }

    /**
     * 重置密码。
     */
    @PutMapping("/{id:\\d+}/password/reset")
    @PreAuthorize("hasAuthority('user:reset_password')")
    @AuditLog(module = "user", action = "RESET_PASSWORD", resourceType = "user", resourceId = "#id")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }

    /**
     * 分配角色。
     */
    @PutMapping("/{id:\\d+}/roles")
    @PreAuthorize("hasAuthority('user:update')")
    @AuditLog(module = "user", action = "ASSIGN_ROLES", resourceType = "user_role", resourceId = "#id")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @RequestBody UserRolesCmd cmd) {
        userService.assignRoles(id, cmd.getRoleIds());
        return Result.ok();
    }

    // ===== 个人中心 =====

    /**
     * 获取当前登录用户信息。
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<UserVO> getCurrentUser() {
        return Result.ok(userService.getCurrentUser());
    }

    /**
     * 更新当前登录用户信息。
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<UserVO> updateCurrentUser(@RequestBody UserUpdateCmd cmd) {
        return Result.ok(userService.updateCurrentUser(cmd));
    }
}
