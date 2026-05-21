package com.manpou.allinone.replenishment.domain.model;

/**
 * 需求单状态枚举（v2.2.0）。
 * - PENDING: 待确认（录入后默认，或取消关联后）
 * - CONFIRMED: 已确认（已关联发注单，由发注单页面关联时写入 linkedProcurementId）
 */
public enum DemandStatus {
    PENDING,     // 待确认
    CONFIRMED,   // 已确认（已关联发注单）
    RETURNED     // 订货失败（终态，由 Procurement 退货联动触发）
}
