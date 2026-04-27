package com.manpou.allinone.customs.domain.model;

/**
 * 国内报关记录状态。
 *
 * 流转规则：
 *   PENDING     — 待提交（初始状态，由用户在 LogisticsPage 手动发起创建，v1.3.0）
 *   SUBMITTED   — 已提交报关
 *   CLEARED     — 通关完成（终态）
 *   REJECTED   — 驳回（可修正后重新提交）
 */
public enum DomesticCustomsStatus {
    PENDING,      // 待提交
    SUBMITTED,    // 已提交
    CLEARED,      // 已通关（终态）
    REJECTED,    // 驳回
}
