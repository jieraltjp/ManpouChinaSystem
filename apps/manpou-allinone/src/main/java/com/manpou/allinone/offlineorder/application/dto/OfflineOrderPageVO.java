package com.manpou.allinone.offlineorder.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfflineOrderPageVO {

    private Long id;
    private Integer showFlag;
    private String code;
    private String subCode;
    private String houkoku;
    private String infoFile;
    private String itemName;
    private Integer volumeCount;
    private Integer orderCount;
    private LocalDate expectedDate;
    private LocalDate orderDate;
    private String arrival;
    private Double unitCh;
    private Double rate;
    private String souko;
    private String factory;
    private String contactor;
    private String contactorTel;
    private String principal;
    private String memo;
    private String link;
    private String updater;
    private LocalDateTime updatetime;
    private BigDecimal inventoryNote;
    private String rireki;
}