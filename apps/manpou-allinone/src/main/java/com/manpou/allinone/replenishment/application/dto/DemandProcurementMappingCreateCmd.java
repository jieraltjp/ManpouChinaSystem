package com.manpou.allinone.replenishment.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 需求-采购分配映射创建命令 DTO（SPEC-B11 §10.2）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandProcurementMappingCreateCmd {

    @NotNull(message = "需求单ID不能为空")
    private Long demandId;

    @NotNull(message = "采购单ID不能为空")
    private Long procurementId;

    @NotNull(message = "分配数量不能为空")
    @Positive(message = "分配数量必须大于0")
    private Integer allocatedQuantity;
}
