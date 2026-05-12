package com.manpou.user.interfaces.controller;

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
    public Result<UserPageVO> pageQuery(UserPageQuery query) {
        return Result.ok(userService.pageQuery(query));
    }

    /**
     * 获取用户详情。
     */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * 新建用户。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public Result<UserVO> create(@Valid @RequestBody UserCreateCmd cmd) {
        return Result.ok(userService.create(cmd));
    }

    /**
     * 更新用户。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<UserVO> update(@PathVariable Long id,
                                  @RequestBody UserUpdateCmd cmd) {
        return Result.ok(userService.update(id, cmd));
    }

    /**
     * 删除用户（软删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /**
     * 启用/禁用用户。
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody UserStatusCmd cmd) {
        userService.updateStatus(id, cmd.getStatus());
        return Result.ok();
    }

    /**
     * 重置密码。
     */
    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasAuthority('user:reset_password')")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }

    /**
     * 分配角色。
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:update')")
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
    public Result<UserVO> getCurrentUser() {
        return Result.ok(userService.getCurrentUser());
    }

    /**
     * 更新当前登录用户信息。
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@RequestBody UserUpdateCmd cmd) {
        return Result.ok(userService.updateCurrentUser(cmd));
    }
}
