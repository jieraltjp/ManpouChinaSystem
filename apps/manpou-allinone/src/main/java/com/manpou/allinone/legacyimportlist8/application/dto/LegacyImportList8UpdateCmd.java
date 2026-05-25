package com.manpou.allinone.legacyimportlist8.application.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class LegacyImportList8UpdateCmd {

    private String code;
    private String manager;
    private String destination;
    private String tax;
    private String material;
    private String kensa;
    private Integer num;
    private Integer pieces;
    private Double weight;
    private Double weight2;
    private Double length;
    private String location;
    private LocalDate date1;
    private String status;
    private String other;
    private Double unitCh;
    private Double rate;
    private String souko;
    private String factoryAddr;
    private Integer showFlag;
    private String rireki;
}
