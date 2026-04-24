package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建补货需求单请求 DTO（v2.0.0）。
 * 一条记录 = 一个子货号（主货号+子货号 = 商品唯一标识）。
 */
@Data
public class ReplenishmentDemandCreateCmd {

    @NotNull(message = "需求类型不能为空")
    private DemandType demandType;

    @NotBlank(message = "主货号不能为空")
    @Length(max = 32)
    private String productCode;

    @NotBlank(message = "子货号不能为空")
    @Length(max = 64)
    private String subProductCode;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    @Max(value = 999999, message = "数量最大为999999")
    private Integer quantity;

    @Length(max = 128)
    private String destination;

    @Length(max = 64)
    private String japanLead;

    @Length(max = 512)
    private String remarks;
}
