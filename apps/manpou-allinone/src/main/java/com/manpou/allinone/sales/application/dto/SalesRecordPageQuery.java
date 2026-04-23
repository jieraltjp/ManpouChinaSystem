package com.manpou.allinone.sales.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalesRecordPageQuery {
    private Long id;
    private String recordCode;
    private Long procurementId;
    private String productCode;
    private String subProductCode;
    private String salesChannel;
    private String status;
    private LocalDate listingDate;
    private Integer initialStock;
    private Integer currentStock;
    private Integer safetyStock;
    private Integer salesQuantity;
    private Integer returnedQuantity;
    private BigDecimal returnRate;
    private BigDecimal sellingPriceJpy;
    private String remarks;
    private String createBy;
    private String createTime;
    private String updateTime;
}
