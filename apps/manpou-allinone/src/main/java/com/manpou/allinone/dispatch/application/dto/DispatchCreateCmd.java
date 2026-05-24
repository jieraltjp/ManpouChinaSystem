package com.manpou.allinone.dispatch.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DispatchCreateCmd {

    @NotBlank(message = "发货编号不能为空")
    private String code;

    @NotBlank(message = "负责人不能为空")
    private String manager;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    private String tax;

    private String material;

    private String kensa;

    @NotNull(message = "数量不能为空")
    private Integer quantity;

    @NotNull(message = "件数不能为空")
    private Integer pieces;

    @NotNull(message = "重量不能为空")
    private Double weight;

    private Double weight2;

    private Double length;

    private String location;

    @NotNull(message = "日期不能为空")
    private LocalDate dispatchDate;

    private String status;

    private String other;

    private Double unitPrice;

    private Double rate;

    private String warehouse;

    private String factoryAddr;

    private Integer showFlag = 0;

    private String rireki;
}