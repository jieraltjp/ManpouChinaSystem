package com.manpou.allinone.procurement.domain.model;

/**
 * 发注单状态枚举。
 * 与 docs/business/SPEC-发注管理流程.md §5 状态推进规则完全对齐。
 *
 * 状态推进规则（与 SPEC §5 一致）：
 * - 永康路径：未定 → 発注待 → 永康 → 倉庫着 → 検品 → エア便/輸出 → 通関 → 日本着 → 会計 → 完了
 * - OEM路径：未定 → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 完了
 * - 直送路径：未定 → 発注待 → 直送 → 倉庫着 → 検品/現地検品 → ...
 *
 * 终态：完了 — 禁止任何状态变更，抛出 BusinessException。
 */
public enum ShipmentStatus {
    未定,       // 还未下单，仅记录需求
    予定,       // 预计发注
    OEM,        // OEM 定制产品路径
    発注待,     // 已录入商品，等待下单
    永康,       // 1688下单后货物发往永康仓
    直送,       // 1688下单后厂家直接发货（不经永康仓）
    倉庫着,     // 货物到达仓库
    現地検品,   // 现场异地验货
    検品,       // 仓库验货
    エア便,     // 空运（尺寸/重量达标的轻量货）
    メーカー直送, // 厂家直送
    輸出,       // 已出口
    通関,       // 已报关
    日本着,     // 已到日本
    会計,       // 财务结算
    完了,       // 全流程结束（终态 — 禁止任何变更）
    退货;       // 退货（独立处理，不影响原单状态）

    /**
     * 终态判断。
     */
    public boolean isTerminal() {
        return this == 完了;
    }

    /**
     * 判断当前状态是否可以转换到目标状态。
     * 对应 SPEC-发注管理流程.md §5 FSM 规则，无效转换返回 false。
     *
     * @param target 目标状态
     * @return 是否允许此转换
     */
    public boolean canTransitionTo(ShipmentStatus target) {
        if (this == target) {
            return true; // 同状态无需更新
        }
        return transitions().getOrDefault(this, java.util.Set.of()).contains(target);
    }

    /**
     * FSM 转换规则表（来源 → 允许目标集合）。
     * 与 SPEC §5 三条路径完全对齐。
     *
     * 永康路径：未定/未定/OEM → 発注待 → 永康/直送 → 倉庫着 → 検品/現地検品 → エア便/輸出 → 通関 → 日本着 → 会計 → 完了
     * OEM路径：未定/未定/OEM → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 完了
     * 直送路径：未定/未定/OEM → 発注待 → 直送 → 倉庫着 → 検品/現地検品 → ...
     * 独立跳转：未定 ↔ 未定
     * 退货：可从任意非完状态进入
     */
    private static java.util.Map<ShipmentStatus, java.util.Set<ShipmentStatus>> transitions() {
        return java.util.Map.ofEntries(
            // ===== 初始状态（可相互跳转，也可进入発注待/OEM）=====
            entry(未定, java.util.Set.of(未定, 予定, 發注待, OEM)),
            entry(予定, java.util.Set.of(未定, 予定, 發注待, OEM)),

            // ===== 発注待分支（永康/直送/OEM 三选一）=====
            entry(OEM,      java.util.Set.of(未定, 予定, 發注待, OEM)),
            entry(發注待, java.util.Set.of(未定, 予定, 永康, 直送, OEM)),

            // ===== 永康路径 =====
            entry(永康,   java.util.Set.of(未定, 予定, 入庫着)),
            entry(直送,   java.util.Set.of(未定, 入庫着)),
            entry(入庫着, java.util.Set.of(未定, 檢品, 現地檢品)),

            // ===== 検品分支 =====
            entry(檢品,      java.util.Set.of(未定, エア便, 輸出, 入庫着)),
            entry(現地檢品, java.util.Set.of(未定, エア便, 輸出, メーカー直送, 入庫着)),

            // ===== 跨境 =====
            entry(エア便,     java.util.Set.of(未定, 通關)),
            entry(輸出,      java.util.Set.of(未定, 通關)),

            // ===== 入日 =====
            entry(通關,   java.util.Set.of(未定, 日本着)),
            entry(日本着, java.util.Set.of(未定, 会計)),
            entry(会計,   java.util.Set.of(未定, 完了)),

            // ===== OEM 终态路径 =====
            entry(メーカー直送, java.util.Set.of(未定, 完了)),

            // ===== 退货（独立状态）=====
            entry(退販, java.util.Set.of(未定, 完了))
        );
    }

    private static <K> java.util.Map.Entry<K, java.util.Set<ShipmentStatus>> entry(K key, java.util.Set<ShipmentStatus> value) {
        return java.util.Map.entry(key, value);
    }

}
