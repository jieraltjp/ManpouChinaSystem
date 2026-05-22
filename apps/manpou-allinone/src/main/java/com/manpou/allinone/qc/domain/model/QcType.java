package com.manpou.allinone.qc.domain.model;

/**
 * 验货方式枚举。
 */
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE,   // 現地検品 — 现场异地验货
    EXEMPT    // 老厂家免验（SPEC-B13）
}
