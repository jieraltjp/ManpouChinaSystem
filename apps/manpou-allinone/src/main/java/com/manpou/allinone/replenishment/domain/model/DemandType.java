package com.manpou.allinone.replenishment.domain.model;

/**
 * 需求类型枚举。
 * 对应业务流第一步：非新品=补货，新品=采购。
 */
public enum DemandType {
    REPLENISHMENT,   // 补货（非新品）
    NEW_PURCHASE      // 新品采购
}
