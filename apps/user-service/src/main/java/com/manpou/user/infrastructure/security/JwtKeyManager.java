package com.manpou.user.infrastructure.security;

import com.manpou.user.domain.port.SigningKeyPort;
import com.manpou.common.security.PemParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * JWT 密钥管理器（环境变量优先，classpath fallback）。
 *
 * 加载顺序：
 * 1. 环境变量 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY（生产/开发推荐）
 * 2. classpath keys/private.pem / keys/public.pem（初始部署 fallback）
 *
 * kid 固定为配置值，无 DB 依赖，避免 DB 与 classpath 密钥不一致的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtKeyManager implements SigningKeyPort {

    @Value("${jwt.key.private:#{null}}")
    private String privateKeyPem;

    @Value("${jwt.key.public:#{null}}")
    private String publicKeyPem;

    @Value("${jwt.key.kid:default-kid}")
    private String kid;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
        log.info("JwtKeyManager initialized, kid={}, source=env", kid);
    }

    private PrivateKey loadPrivateKey() {
        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            return PemParser.parsePrivateKey(privateKeyPem);
        }
        // Fallback: classpath
        try (InputStream is = new ClassPathResource("keys/private.pem").getInputStream()) {
            return PemParser.parsePrivateKey(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException(
                "RSA private key not found: set jwt.key.private env var or place keys/private.pem on classpath", ex);
        }
    }

    private PublicKey loadPublicKey() {
        if (publicKeyPem != null && !publicKeyPem.isBlank()) {
            return PemParser.parsePublicKey(publicKeyPem);
        }
        // Fallback: classpath
        try (InputStream is = new ClassPathResource("keys/public.pem").getInputStream()) {
            return PemParser.parsePublicKey(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException(
                "RSA public key not found: set jwt.key.public env var or place keys/public.pem on classpath", ex);
        }
    }

    public String getCurrentKid() {
        return kid;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /** 当前公钥 PEM（供前端验签 / allinone 跨服务拉取） */
    public String getPublicKeyPem() {
        if (publicKeyPem != null && !publicKeyPem.isBlank()) {
            return publicKeyPem;
        }
        return PemParser.toPublicPem(publicKey);
    }

    /**
     * 根据 kid 获取公钥 PEM。
     * 本实现 kid 固定，忽略入参（AuthController.activePublicKey 使用 getPublicKeyPem 无参版本）。
     */
    public String getPublicKeyPemByKid(String ignoredKid) {
        return getPublicKeyPem();
    }

    @Override
    public void reloadActiveKey() {
        log.warn("Hot-reload not supported in env-based key mode — restart to apply changes");
    }

    public String getActivePublicKeyPem() {
        return getPublicKeyPem();
    }
}
