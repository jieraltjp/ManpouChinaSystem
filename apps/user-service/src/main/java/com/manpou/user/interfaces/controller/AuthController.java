package com.manpou.user.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.common.security.TokenConstants;
import com.manpou.user.domain.model.Permission;
import com.manpou.user.domain.model.Role;
import com.manpou.user.domain.model.User;
import com.manpou.user.domain.repository.PermissionRepository;
import com.manpou.user.domain.repository.RoleRepository;
import com.manpou.user.domain.repository.UserRepository;
import com.manpou.user.infrastructure.security.JwtKeyManager;
import com.manpou.user.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public Result<LoginVO> login(@Valid @RequestBody LoginCmd cmd) {
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

        return Result.ok(new LoginVO(token, TokenConstants.ACCESS_TOKEN_TTL_SECONDS, TokenConstants.BEARER_PREFIX, kid));
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
