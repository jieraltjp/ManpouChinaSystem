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
}
