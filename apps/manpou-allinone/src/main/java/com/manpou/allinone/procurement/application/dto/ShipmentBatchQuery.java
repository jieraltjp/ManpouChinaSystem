package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 出货批次查询参数 DTO（SPEC-B11 §10.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentBatchQuery {

    private Long procurementId;
    private ShipmentBatchStatus status;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer pageSize = 20;
}
