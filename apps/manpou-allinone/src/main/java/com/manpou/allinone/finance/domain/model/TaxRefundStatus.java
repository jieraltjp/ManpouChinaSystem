package com.manpou.allinone.finance.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 退税状态枚举。
 * 流转：APPLYING → COMPLETED | NO_REFUND
 */
@Getter
@RequiredArgsConstructor
public enum TaxRefundStatus {

    APPLYING,    // 退税申请中
    COMPLETED,   // 已退税（终态）
    NO_REFUND;   // 不退税（终态）

    public boolean isTerminal() {
        return this == COMPLETED || this == NO_REFUND;
    }

    public boolean canTransitionTo(TaxRefundStatus target) {
        return switch (this) {
            case APPLYING -> target == COMPLETED || target == NO_REFUND;
            case COMPLETED, NO_REFUND -> false; // 终态不可变更
        };
    }
}
