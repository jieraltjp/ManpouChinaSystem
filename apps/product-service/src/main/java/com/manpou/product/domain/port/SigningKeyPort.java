package com.manpou.product.domain.port;

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
 * @see com.manpou.product.infrastructure.security.JwtKeyManager
 */
public interface SigningKeyPort {

    /**
     * 热加载密钥（轮换后由 KeyManagementService 调用）。
     */
    void reloadActiveKey();

    /** 当前签发密钥 ID。 */
    String getCurrentKid();

    /** 当前公钥 PEM（给前端/其他服务验签用）。 */
    String getPublicKeyPem();
}
