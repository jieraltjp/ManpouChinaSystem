package com.manpou.allinone.legacyprocurement.application.dto;

import lombok.Data;

@Data
public class LegacyProcurementQuery {

    private Integer page = 0;
    private Integer pageSize = 20;
    private String code;
    private String orderGroup;
    private String itemName;
    private String updater;
    private String material;
    private String container;
}