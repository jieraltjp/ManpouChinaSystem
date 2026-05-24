package com.manpou.allinone.offlineorder.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OfflineOrderCreateCmd {

    private Integer showFlag;

    @Size(max = 20)
    private String code;

    @Size(max = 20)
    private String subCode;

    @Size(max = 50)
    private String houkoku;

    @Size(max = 50)
    private String infoFile;

    @Size(max = 100)
    private String itemName;

    private Integer volumeCount;

    private Integer orderCount;

    private LocalDate expectedDate;

    private LocalDate orderDate;

    @Size(max = 20)
    private String arrival;

    private Double unitCh;

    private Double rate;

    @Size(max = 20)
    private String souko;

    @Size(max = 100)
    private String factory;

    @Size(max = 50)
    private String contactor;

    @Size(max = 50)
    private String contactorTel;

    @Size(max = 50)
    private String principal;

    private String memo;

    private String link;

    private BigDecimal inventoryNote;

    private String rireki;
}