package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 出货批次更新命令 DTO（SPEC-B11 §10.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentBatchUpdateCmd {

    private String batchCode;
    private Integer shipmentQuantity;
    private LocalDate factoryShipDate;
    private LocalDate actualShipDate;
    private ShipmentBatchStatus status;
    private String remarks;
}
