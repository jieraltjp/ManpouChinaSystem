package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 创建补货需求单请求 DTO（v1.6.0）。
 * 子货号明细替代旧版 subProductCodes + quantity + destination。
 */
@Data
public class ReplenishmentDemandCreateCmd {

    @NotBlank(message = "需求类型不能为空")
    private DemandType demandType;

    @NotBlank(message = "主货号不能为空")
    @Length(max = 32)
    private String productCode;

    @NotEmpty(message = "子货号明细不能为空")
    @Valid
    private List<SubProductItemDto> subProductItems;

    @Length(max = 64)
    private String japanLead;

    @Length(max = 512)
    private String remarks;
}
