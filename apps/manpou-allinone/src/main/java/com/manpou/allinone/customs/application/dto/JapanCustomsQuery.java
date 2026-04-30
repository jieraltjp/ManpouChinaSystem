package com.manpou.allinone.customs.application.dto;

import lombok.Data;

@Data
public class JapanCustomsQuery {
    private Integer page = 0;
    private Integer pageSize = 20;
    private String status;
    private String containerNo;   // 货柜号（v1.4.0 新增，GET 筛选优先字段）
    private Long procurementId;
    private Long domesticCustomsId;
}
