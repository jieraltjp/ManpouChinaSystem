package com.manpou.allinone.qc.domain.model;

/**
 * 验货记录状态枚举。
 */
public enum QcStatus {
    PENDING,           // 待验货
    COMPLETED,         // 已完成（终态）
    RETURN_REQUESTED   // 发起退货
}
