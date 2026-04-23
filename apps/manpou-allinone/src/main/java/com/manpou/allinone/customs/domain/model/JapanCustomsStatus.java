package com.manpou.allinone.customs.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 日本清关状态枚举。
 * 流转：PENDING → IN_PROGRESS → CLEARED | FAILED
 */
@Getter
@RequiredArgsConstructor
public enum JapanCustomsStatus {

    PENDING,       // 待清关
    IN_PROGRESS,   // 清关中
    CLEARED,       // 已放行（终态）
    FAILED;        // 清关失败（终态）

    public boolean isTerminal() {
        return this == CLEARED || this == FAILED;
    }

    public boolean canTransitionTo(JapanCustomsStatus target) {
        return switch (this) {
            case PENDING -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == CLEARED || target == FAILED;
            case CLEARED, FAILED -> false; // 终态不可变更
        };
    }
}
