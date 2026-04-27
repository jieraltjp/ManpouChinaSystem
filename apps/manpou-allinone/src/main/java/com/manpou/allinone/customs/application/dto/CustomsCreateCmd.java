package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建国内报关记录命令。
 */
@Data
public class CustomsCreateCmd {

    private String containerNo;   // 货柜号（v1.3.0）

    private Long procurementId;

    private Long logisticsPlanId;

    private Long factoryId;

    @NotBlank(message = "货号不能为空")
    @Size(max = 32, message = "货号最多 32 字符")
    private String productCode;

    @Size(max = 64, message = "子货号最多 64 字符")
    private String subProductCode;

    private Integer quantity;

    private BigDecimal estimatedValueCny;

    @Size(max = 512, message = "备注最多 512 字符")
    private String remarks;
}
