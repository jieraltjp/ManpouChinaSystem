package com.manpou.user.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.common.security.TokenConstants;
import com.manpou.user.application.dto.ChangePasswordCmd;
import com.manpou.user.application.service.AuditLogService;
import com.manpou.user.application.service.UserService;
import com.manpou.user.domain.model.AuditLog;
import com.manpou.user.domain.model.Permission;
import com.manpou.user.domain.model.Role;
import com.manpou.user.domain.model.User;
import com.manpou.user.domain.repository.PermissionRepository;
import com.manpou.user.domain.repository.RoleRepository;
import com.manpou.user.domain.repository.UserRepository;
import com.manpou.user.infrastructure.security.JwtContextHolder;
import com.manpou.user.infrastructure.security.JwtKeyManager;
import com.manpou.user.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证接口。
 *
 * 详见 SPEC-B11 §1.5 JWT 跨服务验证架构
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final JwtKeyManager jwtKeyManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserService userService;

    /**
     * 获取当前活跃公钥（前端用于验签 RS256 Token）。
     */
    @GetMapping("/public-key")
    public Result<PublicKeyVO> publicKey() {
        return Result.ok(new PublicKeyVO(
            jwtKeyManager.getCurrentKid(),
            TokenConstants.ALGORITHM_RS256,
            jwtKeyManager.getPublicKeyPem()
        ));
    }

    /**
     * 根据 kid 获取指定公钥（allinone 跨服务验证调用）。
     */
    @GetMapping("/keys/{kid}/public-key")
    public Result<PublicKeyVO> publicKeyByKid(@PathVariable String kid) {
        String pem = jwtKeyManager.getPublicKeyPemByKid(kid);
        return Result.ok(new PublicKeyVO(kid, TokenConstants.ALGORITHM_RS256, pem));
    }

    /**
     * 获取当前活跃公钥（兼容 allinone 调用）。
     */
    @GetMapping("/keys/active/public-key")
    public Result<PublicKeyVO> activePublicKey() {
        return Result.ok(new PublicKeyVO(
            jwtKeyManager.getCurrentKid(),
            TokenConstants.ALGORITHM_RS256,
            jwtKeyManager.getPublicKeyPem()
        ));
    }

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginCmd cmd, HttpServletRequest request) {
        User user = userRepository.findByUsername(cmd.getUsername())
            .orElseThrow(() -> {
                log.warn("Login failed: user not found, username={}", cmd.getUsername());
                return new BusinessException("auth.userNotFound", "用户不存在");
            });

        if (!user.canLogin()) {
            log.warn("Login failed: user cannot login, username={}, status={}, registrationStatus={}",
                cmd.getUsername(), user.getStatus(), user.getRegistrationStatus());
            throw new BusinessException("auth.accountDisabled", "账号已禁用或待审核");
        }

        if (!passwordEncoder.matches(cmd.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: wrong password, username={}", cmd.getUsername());
            throw new BusinessException("auth.wrongPassword", "密码错误");
        }

        // 查询用户角色
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream()
            .map(Role::getRoleCode)
            .collect(Collectors.toList());

        // 查询用户权限
        List<Permission> permissions = permissionRepository.findPermissionsByUserId(user.getId());
        List<String> permissionCodes = permissions.stream()
            .map(Permission::getPermissionCode)
            .collect(Collectors.toList());

        // ADMIN 拥有所有权限（*:*）
        if (roleCodes.contains("ADMIN")) {
            permissionCodes = List.of("*:*");
        }

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // 签发 JWT
        String kid = jwtKeyManager.getCurrentKid();
        String token = jwtService.generateAccessToken(
            String.valueOf(user.getId()),
            user.getUsername(),
            roleCodes,
            permissionCodes,
            "HAIT-001",
            kid
        );

        log.info("User {} logged in, roles={}, permissions={}, kid={}",
            user.getUsername(), roleCodes, permissionCodes.size(), kid);

        // 记录登录日志
        auditLogin(String.valueOf(user.getId()), user.getUsername(), user.getNameCn(), request);

        return Result.ok(new LoginVO(token, TokenConstants.ACCESS_TOKEN_TTL_SECONDS, TokenConstants.BEARER_PREFIX, kid));
    }

    /**
     * 修改当前用户密码。
     * PUT /api/v1/auth/password
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordCmd cmd) {
        userService.changePassword(cmd.getOldPassword(), cmd.getNewPassword());
        return Result.ok();
    }

    /**
     * 用户登出（记录审计日志）。
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> logout(HttpServletRequest request) {
        auditLogout(request);
        return Result.ok();
    }

    // ==================== 审计日志 ====================

    private void auditLogin(String userId, String username, String userName, HttpServletRequest request) {
        try {
            AuditLog al = new AuditLog();
            al.setTraceId(getCurrentTraceId());
            al.setUserId(userId);
            al.setUsername(username);
            al.setOperatorName(userName);
            al.setModule("auth");
            al.setAction("LOGIN");
            al.setHttpMethod("POST");
            al.setHttpUrl("/api/v1/auth/login");
            al.setIpAddress(getClientIp(request));
            al.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            auditLogService.save(al);
        } catch (Exception ex) {
            log.warn("[AuditLog] LOGIN audit failed: userId={}, error={}", userId, ex.getMessage());
        }
    }

    private void auditLogout(HttpServletRequest request) {
        try {
            AuditLog al = new AuditLog();
            al.setTraceId(getCurrentTraceId());
            al.setUserId(JwtContextHolder.getUserId());
            al.setUsername(JwtContextHolder.getUsername());
            al.setOperatorName(JwtContextHolder.getOperatorName());
            al.setModule("auth");
            al.setAction("LOGOUT");
            al.setHttpMethod("POST");
            al.setHttpUrl("/api/v1/auth/logout");
            al.setIpAddress(getClientIp(request));
            al.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            al.setCreateTime(java.time.LocalDateTime.now());
            auditLogService.save(al);
        } catch (Exception ex) {
            log.warn("[AuditLog] LOGOUT audit failed: error={}", ex.getMessage());
        }
    }

    private String getCurrentTraceId() {
        // 从 W3C traceparent header 提取 trace-id
        String tp = org.springframework.web.context.request.RequestContextHolder
            .getRequestAttributes() != null
            ? ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader("traceparent")
            : null;
        if (tp != null && tp.length() >= 35) {
            return tp.substring(3, 35); // 跳过 "00-" 前缀
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }

    // ==================== DTO ====================

    @lombok.Data
    public static class LoginCmd {
        @jakarta.validation.constraints.NotBlank(message = "username is required")
        private String username;

        @jakarta.validation.constraints.NotBlank(message = "password is required")
        private String password;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LoginVO {
        private String accessToken;
        private long expiresIn;
        private String tokenType;
        private String kid;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PublicKeyVO {
        private String kid;
        private String algorithm;
        private String publicKey;
    }

    /**
     * 业务异常。
     */
    public static class BusinessException extends RuntimeException {
        private final String code;

        public BusinessException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
