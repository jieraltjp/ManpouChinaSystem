package com.manpou.allinone.logistics.domain.model;

/**
 * 调配计划状态枚举。
 */
public enum LogisticsStatus {
    PLANNED,      // 已创建
    BOOKED,       // 已订舱
    IN_TRANSIT,   // 运输中
    DELIVERED     // 已送达（终态）
}
