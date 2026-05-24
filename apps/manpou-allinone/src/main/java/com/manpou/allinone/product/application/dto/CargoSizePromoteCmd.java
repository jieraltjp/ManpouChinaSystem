package com.manpou.allinone.product.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CargoSizePromoteCmd {

    @NotBlank(message = "中文名称不能为空")
    private String nameZh;

    private String nameEn;

    private String category = "ORDINARY";

    private String unit = "个";

    private BigDecimal unitPriceRmb;

    private String origin;

    private List<Long> factoryIds;

    private String hsCode;

    private String remarks;
}
