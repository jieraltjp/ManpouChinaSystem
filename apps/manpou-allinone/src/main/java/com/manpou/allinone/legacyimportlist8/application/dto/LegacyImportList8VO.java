package com.manpou.allinone.legacyimportlist8.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegacyImportList8VO {

    private Integer id;
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
    private LocalDateTime updatetime;
    private String updateuser;
    private Integer showFlag;
    private String rireki;
}
