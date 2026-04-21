package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ReplenishmentDemandCreateCmd {

    private DemandType demandType;  // 需求类型（必填）

    @NotBlank(message = "主货号不能为空")
    @Length(max = 32)
    private String productCode;    // 主货号

    @Length(max = 64)
    private String subProductCode; // 子货号/枝番（颜色）

    @NotNull(message = "需求量不能为空")
    @Positive(message = "需求量必须为正数")
    private Integer quantity;     // 需求量

    @Length(max = 128)
    private String destination;   // 目的地

    @Length(max = 64)
    private String japanLead;    // 日本担当

    @Length(max = 512)
    private String remarks;      // 备注
}
