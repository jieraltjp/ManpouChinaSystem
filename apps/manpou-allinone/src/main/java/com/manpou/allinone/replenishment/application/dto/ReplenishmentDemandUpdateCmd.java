package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ReplenishmentDemandUpdateCmd {

    private DemandType demandType;
    private String productCode;

    /** 子货号列表 */
    private List<@Length(max = 64) String> subProductCodes;

    @Positive(message = "需求量必须为正数")
    private Integer quantity;

    @Length(max = 128)
    private String destination;

    @Length(max = 64)
    private String japanLead;

    @Length(max = 512)
    private String remarks;

    private DemandStatus status;  // 仅允许 CANCELLED（取消操作）
}
