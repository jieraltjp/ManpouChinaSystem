package com.manpou.allinone.procurement.domain.model;

/**
 * 出货批次状态枚举（SPEC-B11 §6.1）。
 *
 * FSM：
 *   待验货 ──[创建验货记录并关联]──▶ 验货中 ──[所有 QcRecord COMPLETED]──▶ 已验货
 *         │                              │
 *         │                              └──[新增 QcRecord]──────────▶ 验货中（状态回归）
 *         │
 *         └──[人工取消]──────▶ 已取消
 */
public enum ShipmentBatchStatus {

    待验货,
    验货中,
    已验货,
    已取消;

    /** 终态检查。 */
    public boolean isTerminal() {
        return this == 已验货 || this == 已取消;
    }

    /**
     * 状态转移合法性。
     * 规则：
     *   - 待验货 → 验货中 / 已取消
     *   - 验货中 → 验货中（回归）/ 已验货 / 已取消
     *   - 已验货 → 验货中（撤销）
     *   - 已取消 → 不可转移
     */
    public boolean canTransitionTo(ShipmentBatchStatus target) {
        return switch (this) {
            case 待验货 -> target == 验货中 || target == 已取消;
            case 验货中 -> target == 验货中 || target == 已验货 || target == 已取消;
            case 已验货 -> target == 验货中; // 撤销回归
            case 已取消 -> false;
        };
    }

    /** 判断是否可以删除（仅待验货可删除）。 */
    public boolean canDelete() {
        return this == 待验货;
    }
}
