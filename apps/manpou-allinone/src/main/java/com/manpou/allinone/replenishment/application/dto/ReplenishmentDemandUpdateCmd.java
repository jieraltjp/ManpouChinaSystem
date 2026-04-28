package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 更新补货需求单请求 DTO（v2.2.0）。
 */
@Data
public class ReplenishmentDemandUpdateCmd {

    private DemandType demandType;
    private String productCode;
    private String subProductCode;

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
