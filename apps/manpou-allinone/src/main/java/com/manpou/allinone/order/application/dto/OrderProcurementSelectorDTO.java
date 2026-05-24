package com.manpou.allinone.order.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单选择器中展示的发注单数据。
 * Order 模块独立定义，不依赖 procurement.application.dto。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderProcurementSelectorDTO {

    private Long id;
    private Long factoryId;
    private String factoryName;
    private String productCode;
    private String subProductCode;
    private String material;
    private String requiresQc;
    private Integer quantity;
    private BigDecimal priceRmb;
    private BigDecimal exchangeRate;
    private BigDecimal taxPoint;
    private String billingType;
    private BigDecimal estimatedPriceJpy;
    private String destination;
    private String customerCompany;
    private String status;
    private LocalDate orderDate;
    private LocalDate plannedShipDate;
    private LocalDateTime createTime;
}
