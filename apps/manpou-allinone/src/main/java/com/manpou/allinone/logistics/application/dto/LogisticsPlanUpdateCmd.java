package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LogisticsPlanUpdateCmd {

    private PlanType planType;           // 调配类型

    private LogisticsStatus status;      // 状态

    private BigDecimal cargoLengthCm;

    private BigDecimal cargoWidthCm;

    private BigDecimal cargoHeightCm;

    private BigDecimal cargoWeightKg;

    private BigDecimal netWeightKg;      // 净重(kg)（V49）

    private BigDecimal grossWeightKg;     // 毛重(kg)（V49）

    private Integer quantity;

    private Boolean requiresQc;

    private Long containerId;             // 货柜ID

    @Length(max = 32)
    private String containerNo;          // 货柜号（v1.3.0）

    private Long qcRecordId;               // 关联验货记录（v1.2.0）

    private Long factoryId;                // 关联工厂（v1.3.0）

    private Long poolId;                  // 拼柜池ID

    private LocalDate estimatedShipDate;

    private LocalDate actualShipDate;

    @Length(max = 512)
    private String remarks;

    @Length(max = 64)
    private String customsClearanceNo;    // 通关单号（v2.0）

    private BigDecimal totalWeightKg;     // 货物总重量(kg)（v2.0）

    private BigDecimal totalVolumeCbm;   // 货物总体积(m³)（v2.0）
}
