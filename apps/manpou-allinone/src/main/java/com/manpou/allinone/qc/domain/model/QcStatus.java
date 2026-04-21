package com.manpou.allinone.qc.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * 验货记录状态枚举。
 * FSM 规则：PENDING → COMPLETED | RETURN_REQUESTED，退回终点。
 */
public enum QcStatus {
    PENDING,           // 待验货
    COMPLETED,         // 已完成（终态）
    RETURN_REQUESTED;  // 发起退货

    public boolean isTerminal() {
        return this == COMPLETED || this == RETURN_REQUESTED;
    }

    public boolean canTransitionTo(QcStatus target) {
        if (this == target) return true;
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    private static Map<QcStatus, Set<QcStatus>> transitions() {
        return Map.of(
            PENDING,         Set.of(PENDING, COMPLETED, RETURN_REQUESTED),
            COMPLETED,       Set.of(),
            RETURN_REQUESTED, Set.of()
        );
    }
}
