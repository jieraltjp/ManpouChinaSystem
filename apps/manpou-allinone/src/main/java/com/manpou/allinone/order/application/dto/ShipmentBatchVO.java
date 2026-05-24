package com.manpou.allinone.order.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 厂家出货批次 VO（订单总览详情抽屉用）。
 * 独立于 OrderOverviewPageVO，避免 shared DTO 的耦合问题。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentBatchVO {

    private Long id;

    /** 批次序号（从1起，用于抽屉内展示"批次1/2/3"） */
    private Integer batchIndex;

    private Long procurementId;

    private String batchCode;

    private Integer shipmentQuantity;

    private LocalDate factoryShipDate;

    private LocalDate actualShipDate;

    private ShipmentBatchStatus status;

    private String remarks;

    /** 该批次关联的验货记录数量 */
    private Integer qcRecordCount;

    /** 该批次所有验货记录的合格数量汇总 */
    private Integer totalPassedCount;
}
