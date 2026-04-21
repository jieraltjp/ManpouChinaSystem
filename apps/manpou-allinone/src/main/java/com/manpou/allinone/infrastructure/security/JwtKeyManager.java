package com.manpou.allinone.infrastructure.security;

import com.manpou.allinone.domain.port.SigningKeyPort;
import com.manpou.allinone.common.time.Clock;
import com.manpou.common.security.PemParser;
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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    private static final String LEGACY_PUBLIC_KEY_PATH  = "keys/public.pem";

    private final SigningKeyRepository signingKeyRepository;
    private final Clock clock;

    @Value("${jwt.key.directory:keys}")
    private String keyDirectory;

    private PrivateKey privateKey;
    private PublicKey  publicKey;
    private SigningKey currentKey;

    @PostConstruct
    public void init() {
        loadActiveKey();
    }

    /** 加载当前活跃密钥。优先 DB；无记录则 classpath 引导或生成新密钥。 */
    private void loadActiveKey() {
        Optional<SigningKey> dbKey = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE);

        if (dbKey.isPresent()) {
            currentKey = dbKey.get();
            this.privateKey = loadPrivateKeyFromPath(currentKey.getPrivateKeyPath());
            this.publicKey  = PemParser.parsePublicKey(currentKey.getPublicKeyPem());
            log.info("JWT key loaded from DB: kid={}", currentKey.getKid());
            return;
        }

        log.warn("No active signing key in DB — bootstrapping (first deployment)");
        String kid = UUID.randomUUID().toString().substring(0, 8);

        if (hasClasspathKey()) {
            log.info("Loading bootstrap key from classpath");
            this.privateKey = PemParser.parsePrivateKey(loadPemFromClasspath(LEGACY_PRIVATE_KEY_PATH));
            this.publicKey  = PemParser.parsePublicKey(loadPemFromClasspath(LEGACY_PUBLIC_KEY_PATH));
        } else {
            log.info("Generating new RSA key pair for kid={}", kid);
            KeyPair pair = PemParser.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey  = pair.getPublic();
        }

        SigningKey bootstrap = new SigningKey();
        bootstrap.setKid(kid);
        bootstrap.setPublicKeyPem(PemParser.toPublicPem(publicKey));
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

    /** 热加载密钥（轮换后由 KeyManagementService 调用）。 */
    @Override
    public void reloadActiveKey() {
        loadActiveKey();
        log.info("JWT key hot-reloaded: kid={}", currentKey.getKid());
    }

    // ===== 私钥加载 =====

    private PrivateKey loadPrivateKeyFromPath(String relativePath) {
        // 优先 classpath（兼容打包进 jar 的密钥）
        if (!relativePath.startsWith("/")) {
            try {
                return PemParser.parsePrivateKey(loadPemFromClasspath(relativePath));
            } catch (SecurityException classpathEx) {
                // classpath 没有，尝试文件系统
            }
        }
        // 文件系统路径
        try {
            Path path = Path.of(keyDirectory, relativePath.replace("keys/", ""));
            if (Files.exists(path)) {
                return PemParser.parsePrivateKey(Files.readString(path));
            }
        } catch (SecurityException | IOException fsEx) {
            // ignore
        }
        throw new SecurityException("RSA private key not found: " + relativePath);
    }

    private String loadPemFromClasspath(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new SecurityException(
                "PEM file not found on classpath: " + path, ex);
        }
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
}
