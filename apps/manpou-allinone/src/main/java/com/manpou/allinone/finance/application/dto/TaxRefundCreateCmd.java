package com.manpou.allinone.finance.application.dto;

import com.manpou.allinone.common.enums.BillingType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxRefundCreateCmd {
    private Long procurementId;
    private Long japanCustomsId;
    private BillingType billingType;
    private BigDecimal priceRmb;
    private Integer quantity;
    private BigDecimal taxPoint;
    private BigDecimal exchangeRate;
    private String remarks;
}
