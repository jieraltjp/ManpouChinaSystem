package com.manpou.allinone.legacyprocurement.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LegacyProcurementUpdateCmd {

    private String orderGroup;
    private Integer orderCount;
    private Integer inspectCount;
    private LocalDate yoyakuHasoubi;
    private String arrivalDepo;
    private LocalDate departure;
    private LocalDate arrival;
    private Double unitCh;
    private Double totalCh;
    private Double unitJp;
    private Integer totalJp;
    private Double rate;
    private Integer fbaStock;
    private String houkoku;
    private BigDecimal kaitsuke;
    private BigDecimal hyoten;
    private String kanpu;
    private String container;
    private String boxNum;
    private Integer boxCount;
    private Double kg;
    private Double oneM3;
    private Double allM3;
    private String material;
    private String materialCh;
    private Double height;
    private Double width;
    private Double depth;
    private String note;
    private String receive;
}
