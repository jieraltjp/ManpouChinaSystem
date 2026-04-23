package com.manpou.allinone.sales.application.dto;

import lombok.Data;

@Data
public class SalesRecordQuery {
    private Integer page = 0;
    private Integer pageSize = 20;
    private String productCode;
    private String salesChannel;
    private String status;
    private Long procurementId;
}
