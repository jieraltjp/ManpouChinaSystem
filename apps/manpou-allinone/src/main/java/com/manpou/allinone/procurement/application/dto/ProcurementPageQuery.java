package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发注单分页查询响应 DTO。
 * TODO Phase A: 替换为真实 ShippingOrder 字段（productCode, quantity, estimatedPriceJpy, status 等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementPageQuery {

    private Long id;
    private String name;
    private ShipmentStatus status;
    // TODO Phase A: 追加真实字段
    // private String productCode;
    // private Integer quantity;
    // private BigDecimal estimatedPriceJpy;
    // private LocalDate plannedShipDate;
    // private String productLead;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
