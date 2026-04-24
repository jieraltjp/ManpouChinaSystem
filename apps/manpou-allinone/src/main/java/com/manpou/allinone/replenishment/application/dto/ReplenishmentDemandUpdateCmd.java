package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import jakarta.validation.Valid;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 更新补货需求单请求 DTO（v1.6.0）。
 */
@Data
public class ReplenishmentDemandUpdateCmd {

    private DemandType demandType;
    private String productCode;

    @Valid
    private List<SubProductItemDto> subProductItems;

    @Length(max = 64)
    private String japanLead;

    @Length(max = 512)
    private String remarks;

    /** 仅允许 CANCELLED（取消操作） */
    private DemandStatus status;
}
