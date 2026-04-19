package com.manpou.user.domain.model;

/**
 * 签名密钥状态枚举。
 *
 * ACTIVE: 当前用于签发 Token 的密钥（仅有一个）
 * INACTIVE: 历史密钥，仅用于验签旧 Token
 */
public enum SigningKeyStatus {
    ACTIVE,
    INACTIVE
}
