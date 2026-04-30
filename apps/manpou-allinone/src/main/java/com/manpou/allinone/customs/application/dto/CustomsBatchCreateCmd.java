package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量创建国内报关记录命令（v1.4.0）。
 * 逻辑：一个货柜号下可能有多个 LogisticsPlan，按商品+工厂分别创建报关单。
 * 本命令通过 logisticsPlanIds 批量关联，调配计划字段由 UseCase 层从 LogisticsPlan 实体自动填充。
 */
@Data
public class CustomsBatchCreateCmd {

    /** 货柜号（必填，所有记录共用） */
    @NotBlank(message = "货柜号不能为空")
    private String containerNo;

    /**
     * 要创建报关的 LogisticsPlan ID 列表（必填，至少选一条）。
     * 每个 ID 对应一条 DomesticCustomsRecord，记录字段（productCode/subProductCode/quantity/factoryId 等）
     * 由 UseCase 层从 LogisticsPlan 实体查询填充。
     */
    @NotEmpty(message = "请至少选择一个调配计划")
    private List<@NotNull Long> logisticsPlanIds;

    /** 报关数量（可选，不填则从 LogisticsPlan.quantity 代入） */
    private Integer quantity;

    /** 预估货值 CNY（可选，所有记录共用） */
    private BigDecimal estimatedValueCny;

    /** 备注（可选，所有记录共用） */
    private String remarks;
}
