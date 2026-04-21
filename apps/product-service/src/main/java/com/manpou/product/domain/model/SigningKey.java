package com.manpou.product.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 签名密钥实体（RS256 密钥对元数据）。
 *
 * 存储策略：
 * - 公钥：存储在数据库（供验签和暴露给外部）
 * - 私钥：存储在文件系统（keys/private-{kid}.pem），不进入 DB
 *
 * 生命周期：
 * 1. 生成新密钥对，私钥写入文件系统
 * 2. 公钥 + kid + ACTIVE 状态存入 DB，旧密钥标记为 INACTIVE
 * 3. 新 Token 使用新私钥签发，旧 Token 仍可被旧公钥验签
 *
 * 构造策略：
 * - JPA 持久化：通过反射（{@link jakarta.persistence.Access#FIELD}）读写私有字段
 * - 业务层：新对象通过默认构造器创建，再调用 setter 注入（由领域方法引导）
 *
 * @see <a href="docs/pro/00-root-project.md">认证授权规范 §2.2</a>
 */
@Entity
@Table(name = "signing_key", indexes = {
        @Index(name = "idx_signing_key_status", columnList = "status"),
        @Index(name = "idx_signing_key_kid", columnList = "kid", unique = true)
})
@Access(AccessType.FIELD)
@Getter
public class SigningKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 密钥 ID（kid），写入 JWT header "kid" 字段。
     * 用于验签时定位正确的公钥。
     */
    @Setter
    @Column(name = "kid", nullable = false, unique = true, length = 64)
    private String kid;

    /**
     * 公钥 PEM 内容（不含私钥）。
     * 前端/其他服务用此验签。
     */
    @Setter
    @Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT")
    private String publicKeyPem;

    /**
     * 私钥文件路径（不含私钥内容）。
     * 格式：keys/private-{kid}.pem
     */
    @Setter
    @Column(name = "private_key_path", nullable = false, length = 255)
    private String privateKeyPath;

    /**
     * 密钥状态。
     * ACTIVE：当前签发密钥（仅一个）
     * INACTIVE：历史密钥（保留用于验签旧 Token）
     */
    @Setter
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private SigningKeyStatus status = SigningKeyStatus.INACTIVE;

    /**
     * 密钥创建时间（由审计监听器或 Clock 注入）。
     */
    @Setter
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 默认构造器（JPA 反射实例化 + 业务层引导新对象用）。 */
    public SigningKey() {}

    // ===== 领域方法 =====

    /** 激活此密钥（设为当前签发密钥）。 */
    public void activate() {
        this.status = SigningKeyStatus.ACTIVE;
    }

    /** 停用此密钥（降级为历史密钥）。 */
    public void deactivate() {
        this.status = SigningKeyStatus.INACTIVE;
    }

    /** 判断是否为当前活跃密钥。 */
    public boolean isActive() {
        return SigningKeyStatus.ACTIVE.equals(this.status);
    }
}
