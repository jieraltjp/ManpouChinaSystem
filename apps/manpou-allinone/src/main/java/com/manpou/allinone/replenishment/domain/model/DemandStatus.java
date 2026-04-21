package com.manpou.allinone.replenishment.domain.model;

/**
 * 需求单状态枚举。
 * - PENDING: 待确认（录入后默认状态）
 * - CONVERTED: 已转采购（生成 Procurement 后推进至此）
 * - CANCELLED: 已取消
 */
public enum DemandStatus {
    PENDING,      // 待确认
    CONVERTED,   // 已转采购
    CANCELLED    // 已取消
}
