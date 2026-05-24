package com.manpou.allinone.dispatch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DispatchVO {

    private Long id;
    private String code;
    private String manager;
    private String destination;
    private String tax;
    private String material;
    private String kensa;
    private Integer quantity;
    private Integer pieces;
    private Double weight;
    private Double weight2;
    private Double length;
    private String location;
    private LocalDate dispatchDate;
    private String status;
    private String other;
    private Double unitPrice;
    private Double rate;
    private String warehouse;
    private String factoryAddr;
    private Integer showFlag;
    private String rireki;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}