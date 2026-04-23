package com.manpou.allinone.sales.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 库存不足事件（触发补货需求）。
 * 触发：SalesRecord.currentStock < SalesRecord.safetyStock
 * 副作用：自动创建 ReplenishmentDemand（步骤1）
 */
@Getter
public class ReplenishmentDemandNeededEvent extends ApplicationEvent {

    private final Long salesRecordId;
    private final Long procurementId;
    private final String productCode;
    private final String subProductCode;
    private final Integer requestedQuantity;
    private final Integer currentStock;
    private final Integer safetyStock;

    public ReplenishmentDemandNeededEvent(Object source, Long salesRecordId, Long procurementId,
            String productCode, String subProductCode,
            Integer requestedQuantity, Integer currentStock, Integer safetyStock) {
        super(source);
        this.salesRecordId = salesRecordId;
        this.procurementId = procurementId;
        this.productCode = productCode;
        this.subProductCode = subProductCode;
        this.requestedQuantity = requestedQuantity;
        this.currentStock = currentStock;
        this.safetyStock = safetyStock;
    }
}
