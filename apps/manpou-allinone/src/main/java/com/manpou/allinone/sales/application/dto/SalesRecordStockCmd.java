package com.manpou.allinone.sales.application.dto;

import lombok.Data;

@Data
public class SalesRecordStockCmd {
    private Integer sold;
    private Integer returned;
}
