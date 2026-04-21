package com.manpou.product.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.common.security.TokenConstants;
import com.manpou.product.infrastructure.security.JwtKeyManager;
import com.manpou.product.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证接口。
 *
 * 职责：登录（获取 Token）、刷新 Token、获取公钥。
 * 详见 docs/pro/00-root-project.md §2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final JwtKeyManager jwtKeyManager;

    /**
     * 获取 RSA 公钥（前端用于验签 RS256 Token）。
     * 详见 docs/pro/00-root-project.md §2.2
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
     * 登录（示例实现，需替换为真实用户校验）。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginCmd cmd) {
        // FIXME: scaffold placeholder — 替换为真实用户表查询 + BCrypt 密码校验
        String userId = "user-001";
        String username = cmd.getUsername();
        List<String> roles = List.of("USER");
        List<String> permissions = List.of("example:read", "example:write");
        String tenantId = "tenant-001";

        String kid = jwtKeyManager.getCurrentKid();
        String token = jwtService.generateAccessToken(userId, username, roles, permissions, tenantId, kid);
        log.info("User {} logged in, issued RS256 token (kid={})", username, kid);

        return Result.ok(new LoginVO(token, TokenConstants.ACCESS_TOKEN_TTL_SECONDS, TokenConstants.BEARER_PREFIX, kid));
    }

    // ==================== DTO ====================

    @Data
    public static class LoginCmd {
        @NotBlank(message = "username is required")
        private String username;

        @NotBlank(message = "password is required")
        private String password;
    }

    @Data
    public static class LoginVO {
        private final String accessToken;
        private final long expiresIn;
        private final String tokenType;
        private final String kid;

        public LoginVO(String accessToken, long expiresIn, String tokenType, String kid) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
            this.kid = kid;
        }
    }

    @Data
    public static class PublicKeyVO {
        private final String kid;
        private final String algorithm;
        private final String publicKey;

        public PublicKeyVO(String kid, String algorithm, String publicKey) {
            this.kid = kid;
            this.algorithm = algorithm;
            this.publicKey = publicKey;
        }
    }
}
