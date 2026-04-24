package com.manpou.allinone.replenishment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联发注表明细值对象（v1.6.0）。
 * 嵌入 ReplenishmentDemand.linkedDemandItems JSON 字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkedDemandItem {

    private Long linkedProcurementId;
    private String subCode;
}
