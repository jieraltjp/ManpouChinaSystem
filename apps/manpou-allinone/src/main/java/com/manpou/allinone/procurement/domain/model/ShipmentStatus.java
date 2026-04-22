package com.manpou.allinone.procurement.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * 发注单状态枚举。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §4 状态流转完全对齐。
 *
 * 永康路径：未定 → 発注待 → 永康 → 倉庫着 → 検品 → エア便/輸出 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了
 * OEM路径：未定 → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 日本着 → 日本通関完了 → 会計 → 完了
 * 直送路径：未定 → 発注待 → 直送 → 倉庫着 → 検品/現地検品 → ...
 *
 * 终态：完了 — 禁止任何状态变更，抛出 BusinessException。
 */
public enum ShipmentStatus {
    未定,           // 还未下单，仅记录需求
    予定,           // 预计发注
    OEM,            // OEM 定制产品路径
    発注待,         // 已录入商品，等待下单
    永康,           // 1688下单后货物发往永康仓
    直送,           // 1688下单后厂家直接发货（不经永康仓）
    倉庫着,         // 货物到达仓库
    現地検品,       // 现场异地验货
    検品,           // 仓库验货
    エア便,         // 空运（尺寸/重量达标的轻量货）
    メーカー直送,   // 厂家直送
    輸出,           // 已出口
    国内通関,        // 国内报关（新增 v1.3.0）
    通関,           // 日本报关
    日本着,         // 已到日本
    日本通関完了,    // 日本清关完成（新增 v1.3.0）
    会計,           // 财务结算
    完了,           // 全流程结束（终态 — 禁止任何变更）
    退货;           // 退货（独立处理，不影响原单状态）

    /**
     * 终态判断。
     */
    public boolean isTerminal() {
        return this == 完了;
    }

    /**
     * 判断当前状态是否可以转换到目标状态。
     * 对应 SPEC §5 FSM 规则，无效转换返回 false。
     *
     * @param target 目标状态
     * @return 是否允许此转换
     */
    public boolean canTransitionTo(ShipmentStatus target) {
        if (this == target) {
            return true;
        }
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * FSM 转换规则表。
     * 与 SPEC §5 三条路径（含国内通関/日本通関完了）完全对齐。
     */
    private static Map<ShipmentStatus, Set<ShipmentStatus>> transitions() {
        return Map.ofEntries(
            // 初始状态：未定/未定/OEM 三者可互转，可进入 発注待/OEM
            entry(未定,   Set.of(未定, 予定, 発注待, OEM)),
            entry(予定,   Set.of(未定, 予定, 発注待, OEM)),

            // 発注待分支：永康路径/直送路径/OEM 路径三选一
            entry(OEM,    Set.of(未定, 予定, 発注待, OEM)),
            entry(発注待, Set.of(未定, 予定, 永康, 直送, OEM)),

            // 仓库路径
            entry(永康,   Set.of(未定, 予定, 倉庫着)),
            entry(直送,   Set.of(未定, 倉庫着)),
            entry(倉庫着, Set.of(未定, 検品, 現地検品)),

            // 验货分支
            entry(検品,      Set.of(未定, エア便, 輸出, 倉庫着)),
            entry(現地検品, Set.of(未定, メーカー直送, 倉庫着)),

            // 跨境出口
            entry(エア便,     Set.of(未定, 国内通関)),
            entry(輸出,      Set.of(未定, 国内通関)),
            entry(メーカー直送, Set.of(未定, 日本着)),

            // 报关阶段（含国内通関）
            entry(国内通関, Set.of(未定, 通関)),

            // 日本入境
            entry(通関,   Set.of(未定, 日本着)),
            entry(日本着, Set.of(未定, 日本通関完了)),
            entry(日本通関完了, Set.of(未定, 会計)),

            // 终态前
            entry(会計,   Set.of(未定, 完了)),

            // 退货（独立状态）
            entry(退货, Set.of(未定, 完了))
        );
    }

    private static <K> Map.Entry<K, Set<ShipmentStatus>> entry(K key, Set<ShipmentStatus> value) {
        return Map.entry(key, value);
    }
}
