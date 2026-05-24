package com.manpou.allinone.product.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CargoSizeCreateCmd {

    private String masterCode;
    private String subCode;
    private String code;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal netWeightKg;

    private BigDecimal packHeightCm;
    private BigDecimal packWidthCm;
    private BigDecimal packDepthCm;
    private BigDecimal packageWeightKg;
    private Integer unitsPerPackage;

    private String remarks;
}
