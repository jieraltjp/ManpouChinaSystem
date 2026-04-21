package com.manpou.common.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * PEM 格式解析与编码工具（无状态）。
 * 所有密钥相关类的 PEM 逻辑收敛点。
 *
 * <p>职责：PEM 字符串 ↔ RSA Key 对象相互转换。
 * 生命周期：线程安全，可复用。</p>
 */
public final class PemParser {

    public static final int RSA_KEY_SIZE = 2048;
    private static final String RSA = "RSA";

    private PemParser() {}

    // ===== 解析 =====

    public static PrivateKey parsePrivateKey(String pem) {
        String base64 = stripHeaderFooter(pem, "PRIVATE KEY");
        byte[] keyBytes = decode(base64);
        return buildPrivateKey(keyBytes);
    }

    public static PublicKey parsePublicKey(String pem) {
        String base64 = stripHeaderFooter(pem, "PUBLIC KEY");
        byte[] keyBytes = decode(base64);
        return buildPublicKey(keyBytes);
    }

    // ===== 编码 =====

    public static String toPrivatePem(PrivateKey key) {
        return toPem(key.getEncoded(), "PRIVATE KEY");
    }

    public static String toPublicPem(PublicKey key) {
        return toPem(key.getEncoded(), "PUBLIC KEY");
    }

    // ===== 生成 =====

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA);
            gen.initialize(RSA_KEY_SIZE);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException("RSA algorithm not available", ex);
        }
    }

    // ===== 内部 =====

    private static String stripHeaderFooter(String pem, String marker) {
        return pem.lines()
            .filter(line -> !line.startsWith("-----") && !line.trim().isEmpty())
            .reduce("", (a, b) -> a + b.trim());
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    private static String toPem(byte[] encoded, String marker) {
        String encodedStr = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(encoded).trim();
        return "-----BEGIN " + marker + "-----\n" + encodedStr + "\n-----END " + marker + "-----\n";
    }

    private static PrivateKey buildPrivateKey(byte[] keyBytes) {
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(
                new PKCS8EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new SecurityException("Failed to parse RSA private key", ex);
        }
    }

    private static PublicKey buildPublicKey(byte[] keyBytes) {
        try {
            return KeyFactory.getInstance(RSA).generatePublic(
                new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new SecurityException("Failed to parse RSA public key", ex);
        }
    }
}
