package com.manpou.allinone.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单总览列表行 VO（Phase1 步骤1~4）。
 * 工厂+商品信息来自 procurement_snapshot 快照表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChainVO {

    // ====== 锚点：Demand（步骤1）======
    private Long demandId;
    private String demandCode;
    private String demandType;
    private String demandProductCode;
    private String demandSubProductCode;
    private Integer demandQuantity;
    private String demandDestination;
    private String demandJapanLead;
    private String demandStatus;
    private Long linkedProcurementId;
    private String demandImageUrl;
    private LocalDateTime demandCreateTime;
    private LocalDateTime demandUpdateTime;

    // ====== 快照信息（来自 procurement_snapshot，下单时刻固定不变）======
    private SnapshotVO snapshot;

    // ====== 4步状态（Phase1）======
    private String step1Status;
    private String step2Status;
    private String step3Status;
    private String step4Status;

    // ====== Procurement 信息（列表页直接展示）======
    private String procurementPriceRmb;
    private String procurementTaxPoint;
    private String procurementBillingType;
    private String procurementCreateBy;

    /**
     * 快照 VO（下单时刻的工厂+商品信息）。
     * 来自 procurement_snapshot 表，不随实时数据变化。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotVO {
        private Long factoryId;
        private String factoryCode;
        private String factoryName;
        private String factoryProvince;
        private String factoryCity;
        private String factoryContactName;
        private String factoryContactPhone;
        private String productNameZh;
        private String productNameJa;
        private String productCategory;
    }
}
