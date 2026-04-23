package com.manpou.allinone.customs.application.dto;

import lombok.Data;

@Data
public class JapanCustomsQuery {
    private Integer page = 0;
    private Integer pageSize = 20;
    private String status;
    private Long procurementId;
    private Long domesticCustomsId;
}
