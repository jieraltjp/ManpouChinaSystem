package com.manpou.allinone.infrastructure.security;

import com.manpou.allinone.domain.port.SigningKeyPort;
import com.manpou.allinone.common.time.Clock;
import com.manpou.allinone.domain.model.SigningKey;
import com.manpou.allinone.domain.model.SigningKeyStatus;
import com.manpou.allinone.domain.repository.SigningKeyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Optional;
import java.util.UUID;

/**
 * RSA 密钥管理器（支持密钥轮换）。
 *
 * 密钥存储策略：
 * - 公钥：存储在 signing_key 表
 * - 私钥：存储在文件系统 keys/private-{kid}.pem
 *
 * 启动流程：
 * 1. 查 DB 获取 ACTIVE 密钥
 * 2. 若无记录：从 classpath 引导（兼容旧部署）
 * 3. 若有记录：从文件系统加载对应私钥
 *
 * 详见 docs/pro/19-manpou-allinone.md §认证授权
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtKeyManager implements SigningKeyPort {

    private static final String LEGACY_PRIVATE_KEY_PATH = "keys/private.pem";
    private static final String LEGACY_PUBLIC_KEY_PATH = "keys/public.pem";

    private final SigningKeyRepository signingKeyRepository;
    private final Clock clock;

    @Value("${jwt.key.directory:keys}")
    private String keyDirectory;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SigningKey currentKey;

    @PostConstruct
    public void init() {
        loadActiveKey();
    }

    /**
     * 加载当前活跃密钥。
     * 优先从 DB 加载；无记录则从 classpath 引导。
     */
    private void loadActiveKey() {
        Optional<SigningKey> dbKey = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE);

        if (dbKey.isPresent()) {
            currentKey = dbKey.get();
            this.privateKey = loadPrivateKeyFromPath(currentKey.getPrivateKeyPath());
            this.publicKey = parsePublicKey(currentKey.getPublicKeyPem());
            log.info("JWT key loaded from DB: kid={}", currentKey.getKid());
            return;
        }

        // 引导：无 DB 记录 → 尝试从 classpath 加载旧密钥；若 classpath 也没有 → 生成新密钥
        log.warn("No active signing key in DB — bootstrapping (first deployment)");
        String kid = UUID.randomUUID().toString().substring(0, 8);

        if (hasClasspathKey()) {
            log.info("Loading bootstrap key from classpath");
            this.privateKey = loadPrivateKeyFromClasspath(LEGACY_PRIVATE_KEY_PATH);
            this.publicKey = loadPublicKeyFromClasspath(LEGACY_PUBLIC_KEY_PATH);
        } else {
            log.info("Generating new RSA key pair for kid={}", kid);
            KeyPair pair = generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
        }

        SigningKey bootstrap = new SigningKey();
        bootstrap.setKid(kid);
        bootstrap.setPublicKeyPem(toPem(publicKey, "PUBLIC KEY"));
        bootstrap.setPrivateKeyPath(LEGACY_PRIVATE_KEY_PATH);
        bootstrap.setStatus(SigningKeyStatus.ACTIVE);
        bootstrap.setCreateTime(clock.nowLocalDateTime());
        currentKey = signingKeyRepository.save(bootstrap);
        log.info("JWT bootstrap key persisted: kid={}", kid);
    }

    /** 当前签发密钥 ID。 */
    public String getCurrentKid() {
        return currentKey.getKid();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /** 当前公钥 PEM（给前端验签用）。 */
    public String getPublicKeyPem() {
        return currentKey.getPublicKeyPem();
    }

    /**
     * 热加载密钥（轮换后由 KeyManagementService 调用）。
     */
    public void reloadActiveKey() {
        loadActiveKey();
        log.info("JWT key hot-reloaded: kid={}", currentKey.getKid());
    }

    // ===== 私钥加载 =====

    private PrivateKey loadPrivateKeyFromPath(String relativePath) {
        // 优先检查 classpath（兼容打包进 jar 的密钥）
        if (!relativePath.startsWith("/")) {
            try (InputStream is = new ClassPathResource(relativePath).getInputStream()) {
                return parsePrivateKey(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException classpathEx) {
                // classpath 没有，尝试文件系统
            }
        }
        // 文件系统路径（绝对路径或外部挂载路径）
        try {
            Path path = Path.of(keyDirectory, relativePath.replace("keys/", ""));
            if (Files.exists(path)) {
                return parsePrivateKey(Files.readString(path));
            }
        } catch (IOException fsEx) {
            // ignore
        }
        throw new IllegalStateException("RSA private key not found: " + relativePath);
    }

    private PrivateKey loadPrivateKeyFromClasspath(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return parsePrivateKey(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException(
                "RSA private key not found on classpath: " + path, ex);
        }
    }

    private PublicKey loadPublicKeyFromClasspath(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parsePublicKey(pem);
        } catch (IOException ex) {
            throw new IllegalStateException(
                "RSA public key not found on classpath: " + path, ex);
        }
    }

    // ===== PEM 解析 =====

    private PrivateKey parsePrivateKey(String pem) {
        try {
            String base64 = stripPemHeaderFooter(pem, "PRIVATE KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Failed to parse RSA private key", ex);
        }
    }

    private PublicKey parsePublicKey(String pem) {
        try {
            String base64 = stripPemHeaderFooter(pem, "PUBLIC KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Failed to parse RSA public key", ex);
        }
    }

    private String stripPemHeaderFooter(String pem, String marker) {
        return pem.lines()
            .filter(line -> !line.startsWith("-----") && !line.trim().isEmpty())
            .reduce("", (a, b) -> a + b.trim());
    }

    private String toPem(PublicKey key, String marker) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(key.getEncoded()).trim();
        return "-----BEGIN " + marker + "-----\n" + encoded + "\n-----END " + marker + "-----\n";
    }

    private boolean hasClasspathKey() {
        try {
            new ClassPathResource(LEGACY_PRIVATE_KEY_PATH).getInputStream().close();
            new ClassPathResource(LEGACY_PUBLIC_KEY_PATH).getInputStream().close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }
}
