package com.manpou.allinone.factory.domain.model;

/**
 * 账期枚举。
 * 对应 DB-10 §2.3
 */
public enum PaymentTerms {
    CASH,   // 现结
    NET_30, // 月结30天
    NET_60, // 月结60天
    NET_90, // 月结90天
    CREDIT  // 信用账期
}
