package com.manpou.allinone.logistics.domain.model;

/**
 * 拼柜池状态枚举。
 * FSM 规则：
 *   OPEN     → OPEN（可重复设置）/ PENDING（手动封池）/ LOADED（自动触发装柜后）
 *   PENDING  → PENDING / LOADED（装柜完成）
 *   LOADED   → SHIPPED（出港）
 *   SHIPPED  → 终态
 */
public enum ConsolidationPoolStatus {
    OPEN,     // 开放入库
    PENDING,  // 待装柜（手动封池）
    LOADED,   // 已装柜
    SHIPPED;  // 已出港（终态）

    public boolean isTerminal() {
        return this == SHIPPED;
    }

    public boolean canTransitionTo(ConsolidationPoolStatus target) {
        if (this == target) return true;
        return switch (this) {
            case OPEN -> target == PENDING || target == LOADED;
            case PENDING -> target == LOADED;
            case LOADED -> target == SHIPPED;
            case SHIPPED -> false;
        };
    }
}
