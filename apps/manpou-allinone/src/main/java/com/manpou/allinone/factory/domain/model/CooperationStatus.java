package com.manpou.allinone.factory.domain.model;

/**
 * 合作状态枚举。
 * 对应 DB-10 §2.2
 */
public enum CooperationStatus {
    ACTIVE,     // 合作中
    SUSPENDED,  // 已暂停
    ELIMINATED, // 已淘汰
    POTENTIAL   // 潜在合作
}
