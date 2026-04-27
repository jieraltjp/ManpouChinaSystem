package com.manpou.allinone.customs.application.dto;

import com.manpou.allinone.customs.domain.model.DomesticCustomsStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新国内报关记录命令。
 */
@Data
public class CustomsUpdateCmd {

    private String containerNo;   // 货柜号（v1.3.0）

    private Long factoryId;

    @Size(max = 32, message = "货号最多 32 字符")
    private String productCode;

    @Size(max = 64, message = "子货号最多 64 字符")
    private String subProductCode;

    private Integer quantity;

    private BigDecimal estimatedValueCny;

    @Size(max = 512, message = "备注最多 512 字符")
    private String remarks;
}
