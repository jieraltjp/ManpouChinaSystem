package com.manpou.allinone.replenishment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子货号明细值对象（v1.6.0）。
 * 嵌入 ReplenishmentDemand.subProductItems JSON 字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubProductItem {

    private String subCode;
    private Integer quantity;
    private String destination;

    public SubProductItem(String subCode) {
        this.subCode = subCode;
        this.quantity = 0;
        this.destination = null;
    }
}
