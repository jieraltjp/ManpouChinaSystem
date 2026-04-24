package com.manpou.allinone.replenishment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 补货需求单分页查询响应 DTO（v1.6.0）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplenishmentDemandPageQuery {

    private Long id;
    private String demandCode;
    private DemandType demandType;
    private String productCode;
    /** 子货号明细列表（v1.6.0，每个含 subCode + quantity + destination） */
    private List<SubProductItem> subProductItems;
    private String japanLead;
    private DemandStatus status;
    /** 关联发注表明细列表（v1.6.0，CONVERTED 时填充） */
    private List<com.manpou.allinone.replenishment.domain.model.LinkedDemandItem> linkedDemandItems;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
