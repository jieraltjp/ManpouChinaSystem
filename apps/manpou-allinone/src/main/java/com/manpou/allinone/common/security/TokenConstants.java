package com.manpou.allinone.common.security;

/**
 * JWT 认证相关常量。
 */
public final class TokenConstants {

    private TokenConstants() {}

    /** 签发算法 */
    public static final String ALGORITHM_RS256 = "RS256";

    /** Token 类型（RFC 6750） */
    public static final String BEARER_PREFIX = "Bearer";

    /** Access Token 有效期（秒），1 天 */
    public static final long ACCESS_TOKEN_TTL_SECONDS = 86400;

    /** 密钥 classpath 引导路径（旧部署兼容） */
    public static final String LEGACY_PRIVATE_KEY_PATH = "keys/private.pem";
    public static final String LEGACY_PUBLIC_KEY_PATH  = "keys/public.pem";

    /** JWT claim 名称 */
    public static final String CLAIM_KID   = "kid";
    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_SUB   = "sub";
}
