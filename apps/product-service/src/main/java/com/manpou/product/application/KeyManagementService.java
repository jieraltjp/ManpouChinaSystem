package com.manpou.product.application;

import com.manpou.product.domain.port.SigningKeyPort;
import com.manpou.common.time.Clock;
import com.manpou.common.security.PemParser;
import com.manpou.product.domain.model.SigningKey;
import com.manpou.product.domain.model.SigningKeyStatus;
import com.manpou.product.domain.repository.SigningKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.manpou.product.common.exception.BusinessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 密钥管理服务（应用层编排）。
 *
 * 职责：
 * - 生成 RSA 2048 密钥对
 * - 私钥写入文件系统，公钥 + 元数据存入 DB
 * - 原子性切换活跃密钥（停旧 → 启新 → 热加载）
 *
 * 安全说明：私钥永远不存入数据库，不经过网络传输。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyManagementService {


    private final SigningKeyRepository signingKeyRepository;
    private final SigningKeyPort signingKeyPort;
    private final Clock clock;

    @Value("${jwt.key.directory:keys}")
    private String keyDirectory;

    /**
     * 轮换密钥：停用当前密钥，生成并激活新密钥。
     *
     * @return 新密钥 ID
     */
    @Transactional
    public String rotateKey() {
        // 1. 停用当前活跃密钥
        signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)
            .ifPresent(key -> {
                key.deactivate();
                signingKeyRepository.save(key);
                log.info("Previous key deactivated: kid={}", key.getKid());
            });

        // 2. 生成新密钥对
        String kid = UUID.randomUUID().toString().substring(0, 8);
        KeyPairResult keyPair = generateKeyPair(kid);

        // 3. 私钥写入文件系统
        writePrivateKey(kid, keyPair.privateKeyPem());

        // 4. 公钥 + 元数据存入 DB
        SigningKey newKey = new SigningKey();
        newKey.setKid(kid);
        newKey.setPublicKeyPem(keyPair.publicKeyPem());
        newKey.setPrivateKeyPath("keys/private-" + kid + ".pem");
        newKey.activate();
        newKey.setCreateTime(clock.nowLocalDateTime());
        SigningKey saved = signingKeyRepository.save(newKey);

        // 5. 热加载新密钥到 JwtKeyManager（通过 Port 接口）
        signingKeyPort.reloadActiveKey();

        log.info("Key rotated: newKid={}", saved.getKid());
        return saved.getKid();
    }

    /**
     * 查询所有密钥元数据（不含私钥）。
     */
    public List<KeyInfo> listKeys() {
        return signingKeyRepository.findAllByOrderByCreateTimeDesc()
            .stream()
            .map(k -> new KeyInfo(k.getKid(), k.getStatus().name(), k.getCreateTime()))
            .toList();
    }

    // ===== 内部方法 =====

    private KeyPairResult generateKeyPair(String kid) {
        java.security.KeyPair pair = PemParser.generateKeyPair();
        return new KeyPairResult(PemParser.toPrivatePem(pair.getPrivate()), PemParser.toPublicPem(pair.getPublic()));
    }

    private void writePrivateKey(String kid, String privateKeyPem) {
        try {
            Path dir = Path.of(keyDirectory);
            Files.createDirectories(dir);
            Path file = dir.resolve("private-" + kid + ".pem");
            Files.writeString(file, privateKeyPem, StandardCharsets.UTF_8);
            log.debug("Private key written: {}", file);
        } catch (IOException ex) {
            throw new BusinessException("key.filesystem-error", "Failed to write private key: " + ex.getMessage());
        }
    }



    // ===== 内部记录 =====

    public record KeyPairResult(String privateKeyPem, String publicKeyPem) {}
    public record KeyInfo(String kid, String status, LocalDateTime createTime) {}
}
