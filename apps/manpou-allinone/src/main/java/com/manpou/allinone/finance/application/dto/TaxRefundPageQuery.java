package com.manpou.allinone.finance.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TaxRefundPageQuery {

    private Long id;
    private String refundCode;
    private Long procurementId;
    private Long japanCustomsId;
    private String status;
    private String billingType;
    private BigDecimal priceRmb;
    private Integer quantity;
    private BigDecimal taxPoint;
    private BigDecimal estimatedRefundRmb;
    private BigDecimal actualRefundRmb;
    private BigDecimal exchangeRate;
    private LocalDate refundDate;
    private String refundBank;
    private String remarks;
    private String createBy;
    private String createTime;
    private String updateTime;
    // 产品信息（通过 procurementId JOIN procurement + product 获取）
    private String productCode;
    private String subProductCode;
    private String productImageUrl;
}
