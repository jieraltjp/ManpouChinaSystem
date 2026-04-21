package com.manpou.allinone.logistics.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * 调配计划状态枚举。
 * FSM 规则：PLANNED → BOOKED → IN_TRANSIT → DELIVERED（线性，禁止倒退）。
 */
public enum LogisticsStatus {
    PLANNED,      // 已创建
    BOOKED,       // 已订舱
    IN_TRANSIT,   // 运输中
    DELIVERED;    // 已送达（终态）

    public boolean isTerminal() {
        return this == DELIVERED;
    }

    public boolean canTransitionTo(LogisticsStatus target) {
        if (this == target) return true;
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    private static Map<LogisticsStatus, Set<LogisticsStatus>> transitions() {
        return Map.of(
            PLANNED,    Set.of(PLANNED, BOOKED),
            BOOKED,    Set.of(BOOKED, IN_TRANSIT),
            IN_TRANSIT, Set.of(IN_TRANSIT, DELIVERED),
            DELIVERED, Set.of()
        );
    }
}
