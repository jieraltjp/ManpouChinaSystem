package com.manpou.allinone.logistics.domain.model;

/**
 * 货柜状态枚举。
 * FSM 规则：
 *   CREATED → LOADED → DEPARTED → ARRIVED
 *   线性，禁止倒退
 */
public enum ContainerStatus {
    CREATED,   // 货柜已创建
    LOADED,    // 已装柜
    DEPARTED,  // 已离港
    ARRIVED;   // 已到港（终态）

    public boolean isTerminal() {
        return this == ARRIVED;
    }

    public boolean canTransitionTo(ContainerStatus target) {
        if (this == target) return true;
        return switch (this) {
            case CREATED -> target == LOADED;
            case LOADED -> target == DEPARTED;
            case DEPARTED -> target == ARRIVED;
            case ARRIVED -> false;
        };
    }
}
