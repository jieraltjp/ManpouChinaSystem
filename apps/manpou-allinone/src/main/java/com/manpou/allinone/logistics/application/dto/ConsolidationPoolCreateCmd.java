package com.manpou.allinone.logistics.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Data
public class ConsolidationPoolCreateCmd {

    @NotBlank(message = "目的港不能为空")
    @Length(max = 64)
    private String destinationPort;

    @Length(max = 10)
    private BigDecimal containerThresholdCbm;  // 触发阈值，默认 70m³
}
