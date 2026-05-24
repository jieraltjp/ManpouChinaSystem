package com.manpou.allinone.procurement.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出货批次分页查询响应 DTO（SPEC-B11 §10.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentBatchPageQuery {

    private Long id;
    private Long procurementId;
    private String batchCode;
    private Integer shipmentQuantity;
    private LocalDate factoryShipDate;
    private LocalDate actualShipDate;
    private ShipmentBatchStatus status;
    private String remarks;
    private Integer qcRecordCount;
    private Integer totalPassedCount;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    // 商品信息（通过 procurement.productCode = nameZh 关联 product 表获取）
    private String productMasterCode;
    private String productSubCode;
    private String productImageUrl;
}
