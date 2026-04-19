package com.manpou.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT 公钥管理器（网关只验签，不需要私钥）。
 *
 * 从 classpath:keys/public.pem 加载 RS256 公钥。
 * 与 java-service 的 JwtKeyManager.getPublicKey() 保持一致。
 */
@Slf4j
@Component
public class JwtPublicKeyManager {

    private static final String PUBLIC_KEY_PATH = "keys/public.pem";

    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        this.publicKey = loadPublicKey();
        log.info("JWT RS256 public key loaded for gateway verification");
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey loadPublicKey() {
        try (InputStream is = new ClassPathResource(PUBLIC_KEY_PATH).getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parsePublicKey(pem);
        } catch (Exception ex) {
            throw new IllegalStateException(
                "RSA public key not found at classpath:" + PUBLIC_KEY_PATH, ex);
        }
    }

    private PublicKey parsePublicKey(String pem) {
        try {
            String base64 = stripPemHeaderFooter(pem, "PUBLIC KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Failed to parse RSA public key", ex);
        }
    }

    private String stripPemHeaderFooter(String pem, String marker) {
        return pem.lines()
            .filter(line -> !line.startsWith("-----") && !line.trim().isEmpty())
            .reduce("", (a, b) -> a + b.trim());
    }
}
