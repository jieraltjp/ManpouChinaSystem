package com.manpou.allinone.offlineorder.application.dto;

import lombok.Data;

@Data
public class OfflineOrderQuery {

    private Integer page = 0;
    private Integer pageSize = 20;
    private String code;
    private String itemName;
    private String factory;
    private String arrival;
}