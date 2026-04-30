package com.manpou.allinone.procurement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 出货批次创建命令 DTO（SPEC-B11 §10.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentBatchCreateCmd {

    @NotNull(message = "采购单ID不能为空")
    private Long procurementId;

    @NotBlank(message = "批次编号不能为空")
    private String batchCode;

    @NotNull(message = "出货数量不能为空")
    @Positive(message = "出货数量必须大于0")
    private Integer shipmentQuantity;

    /** 厂家发货日。 */
    private LocalDate factoryShipDate;

    /** 备注。 */
    private String remarks;
}
