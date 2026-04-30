package com.manpou.allinone.domain.port;

/**
 * 签名密钥管理端口（Domain 层接口）。
 *
 * <p>定义 Application 层需要的密钥管理抽象，Infrastructure 层实现。</p>
 *
 * <p>为何放在 Domain 层？
 * Port 定义了领域服务对外部基础设施的需求，属于领域接口。
 * Infrastructure 层（JwtKeyManager）实现此接口，
 * Application 层（KeyManagementService）通过此接口与 Infrastructure 解耦。</p>
 *
 * <p>只读模式（方案B）：allinone 仅从 user-service 拉取公钥验证，不签发。</p>
 *
 * @see com.manpou.allinone.infrastructure.security.JwtKeyManager
 */
public interface SigningKeyPort {

    /**
     * 热加载公钥（从 user-service 拉取最新活跃公钥）。
     */
    void reloadActiveKey();

    /** 当前 kid（从缓存获取）。 */
    String getCurrentKid();

    /** 当前活跃公钥 PEM。 */
    String getActivePublicKeyPem();
}
