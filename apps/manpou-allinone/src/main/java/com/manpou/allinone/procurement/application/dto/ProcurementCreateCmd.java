package com.manpou.allinone.procurement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建发注单命令对象。
 * TODO Phase A: 替换为真实 ShippingOrder 字段（productCode, quantity, priceRmb, exchangeRate, taxPoint 等）。
 */
@Data
public class ProcurementCreateCmd {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称最多 128 字符")
    private String name;
}
