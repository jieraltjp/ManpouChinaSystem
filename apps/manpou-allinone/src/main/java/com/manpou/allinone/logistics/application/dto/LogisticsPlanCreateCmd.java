package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LogisticsPlanCreateCmd {

    private Long qcRecordId;              // 关联验货记录（调配锚点，v1.2.0）

    private Long procurementId;          // 关联采购单（可选，拼柜场景）

    private Long factoryId;               // 关联工厂

    @NotNull(message = "货号不能为空")
    @Length(max = 32)
    private String productCode;           // 货号

    @Length(max = 64)
    private String subProductCode;        // 子货号/颜色

    @NotNull(message = "调配类型不能为空")
    private PlanType planType;            // 调配类型

    private BigDecimal cargoLengthCm;     // 长(cm)

    private BigDecimal cargoWidthCm;     // 宽(cm)

    private BigDecimal cargoHeightCm;    // 高(cm)

    private BigDecimal cargoWeightKg;    // 重量(kg)

    private Integer quantity;             // 数量

    private Boolean requiresQc;           // 是否需要检测

    private LocalDate estimatedShipDate;  // 预计发货日

    private LocalDate actualShipDate;     // 实际发货日

    @Length(max = 512)
    private String remarks;               // 备注
}
