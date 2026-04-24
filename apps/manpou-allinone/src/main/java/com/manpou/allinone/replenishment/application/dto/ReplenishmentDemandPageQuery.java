package com.manpou.allinone.replenishment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 补货需求单分页查询响应 DTO（v2.0.0）。
 * 一行 = 一个子货号（商品唯一标识 = 主货号+子货号）。
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
    /** 主货号 */
    private String productCode;
    /** 子货号（全码，如 ad009-be） */
    private String subProductCode;
    /** 需求数量 */
    private Integer quantity;
    /** 目的地 */
    private String destination;
    private String japanLead;
    private DemandStatus status;
    /** 关联的 Procurement ID（CONVERTED 时有值） */
    private Long linkedProcurementId;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
