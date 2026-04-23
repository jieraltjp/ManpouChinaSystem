package com.manpou.allinone.finance.application.dto;

import lombok.Data;

@Data
public class TaxRefundQuery {
    private Integer page = 0;
    private Integer pageSize = 20;
    private String status;
    private Long procurementId;
}
