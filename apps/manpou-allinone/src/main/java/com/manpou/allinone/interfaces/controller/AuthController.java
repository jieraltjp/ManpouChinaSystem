package com.manpou.allinone.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.common.security.TokenConstants;
import com.manpou.allinone.infrastructure.security.JwtKeyManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口（只读模式）。
 *
 * allinone 只验证 Token，不签发。签发由 user-service 负责。
 * 详见 SPEC-B11 §1.5 JWT 跨服务验证架构（方案B）
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtKeyManager jwtKeyManager;

    public AuthController(JwtKeyManager jwtKeyManager) {
        this.jwtKeyManager = jwtKeyManager;
    }

    /**
     * 获取当前活跃公钥（供内部服务使用，前端请调用 user-service）。
     */
    @GetMapping("/public-key")
    public Result<PublicKeyVO> publicKey() {
        return Result.ok(new PublicKeyVO(
            jwtKeyManager.getCurrentKid(),
            TokenConstants.ALGORITHM_RS256,
            jwtKeyManager.getActivePublicKeyPem()
        ));
    }

    // ==================== DTO ====================

    public static class PublicKeyVO {
        private final String kid;
        private final String algorithm;
        private final String publicKey;

        public PublicKeyVO(String kid, String algorithm, String publicKey) {
            this.kid = kid;
            this.algorithm = algorithm;
            this.publicKey = publicKey;
        }

        public String getKid() { return kid; }
        public String getAlgorithm() { return algorithm; }
        public String getPublicKey() { return publicKey; }
    }
}
