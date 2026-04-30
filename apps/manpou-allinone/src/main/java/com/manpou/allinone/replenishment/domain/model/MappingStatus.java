package com.manpou.allinone.replenishment.domain.model;

/**
 * 需求-采购分配映射状态枚举（SPEC-B11 §6.2）。
 *
 * 状态：
 *   进行中 — 分配已建立，等待验货完成
 *   已完成 — 验货合格量 ≥ allocatedQuantity
 *   已取消 — 取消本次分配
 */
public enum MappingStatus {

    进行中,
    已完成,
    已取消;

    /** 终态检查。 */
    public boolean isTerminal() {
        return this == 已完成 || this == 已取消;
    }
}
