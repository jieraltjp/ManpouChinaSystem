package com.manpou.allinone.procurement.application;

import com.manpou.allinone.common.time.Clock;
import com.manpou.allinone.domain.model.SigningKey;
import com.manpou.allinone.domain.model.SigningKeyStatus;
import com.manpou.allinone.domain.port.SigningKeyPort;
import com.manpou.allinone.domain.repository.SigningKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 密钥管理服务（采购模块实例）。
 * 逻辑与 product/warehouse 等模块同步。
 */
@Service("procurementKeyManagementService")
public class KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementService.class);
    private static final int RSA_KEY_SIZE = 2048;

    private final SigningKeyRepository signingKeyRepository;
    private final SigningKeyPort signingKeyPort;
    private final Clock clock;

    public KeyManagementService(SigningKeyRepository signingKeyRepository,
                                SigningKeyPort signingKeyPort,
                                Clock clock) {
        this.signingKeyRepository = signingKeyRepository;
        this.signingKeyPort = signingKeyPort;
        this.clock = clock;
    }

    @Value("${jwt.key.directory:keys}")
    private String keyDirectory;

    @Transactional
    public String rotateKey() {
        signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)
            .ifPresent(key -> {
                key.deactivate();
                signingKeyRepository.save(key);
                log.info("Previous key deactivated: kid={}", key.getKid());
            });

        String kid = UUID.randomUUID().toString().substring(0, 8);
        KeyPairResult keyPair = generateKeyPair(kid);
        writePrivateKey(kid, keyPair.privateKeyPem());

        SigningKey newKey = new SigningKey();
        newKey.setKid(kid);
        newKey.setPublicKeyPem(keyPair.publicKeyPem());
        newKey.setPrivateKeyPath("keys/private-" + kid + ".pem");
        newKey.activate();
        newKey.setCreateTime(clock.nowLocalDateTime());
        SigningKey saved = signingKeyRepository.save(newKey);
        signingKeyPort.reloadActiveKey();

        log.info("Key rotated: newKid={}", saved.getKid());
        return saved.getKid();
    }

    public List<KeyInfo> listKeys() {
        return signingKeyRepository.findAllOrderByCreateTimeDesc()
            .stream()
            .map(k -> new KeyInfo(k.getKid(), k.getStatus().name(), k.getCreateTime()))
            .toList();
    }

    private KeyPairResult generateKeyPair(String kid) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(RSA_KEY_SIZE);
            java.security.KeyPair pair = gen.generateKeyPair();
            return new KeyPairResult(toPrivatePem(pair.getPrivate()), toPublicPem(pair.getPublic()));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }

    private void writePrivateKey(String kid, String privateKeyPem) {
        try {
            Path dir = Path.of(keyDirectory);
            Files.createDirectories(dir);
            Path file = dir.resolve("private-" + kid + ".pem");
            Files.writeString(file, privateKeyPem, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write private key", ex);
        }
    }

    private String toPrivatePem(PrivateKey key) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(key.getEncoded()).trim();
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
    }

    private String toPublicPem(PublicKey key) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(key.getEncoded()).trim();
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";
    }

    public record KeyPairResult(String privateKeyPem, String publicKeyPem) {}
    public record KeyInfo(String kid, String status, LocalDateTime createTime) {}
}
