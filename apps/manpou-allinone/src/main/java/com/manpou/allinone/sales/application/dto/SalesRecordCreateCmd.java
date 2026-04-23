package com.manpou.allinone.sales.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesRecordCreateCmd {
    private Long procurementId;
    private String productCode;
    private String subProductCode;
    private String salesChannel;
    private LocalDate listingDate;
    private Integer initialStock;
    private Integer safetyStock;
    private BigDecimal sellingPriceJpy;
    private String remarks;
}
