package com.manpou.allinone.logistics.domain.event;

import lombok.Getter;

/**
 * 调配计划进入运输中状态事件。
 *
 * 触发：LogisticsPlan.status → IN_TRANSIT
 * 消费：创建 DomesticCustomsRecord（国内报关）
 *
 * 使用 Spring ApplicationEventPublisher 广播。
 */
@Getter
public class LogisticsPlanInTransitEvent {

    private final Long logisticsPlanId;
    private final String planCode;
    private final Long procurementId;
    private final Long factoryId;
    private final String productCode;
    private final String subProductCode;

    public LogisticsPlanInTransitEvent(
            Long logisticsPlanId,
            String planCode,
            Long procurementId,
            Long factoryId,
            String productCode,
            String subProductCode) {
        this.logisticsPlanId = logisticsPlanId;
        this.planCode = planCode;
        this.procurementId = procurementId;
        this.factoryId = factoryId;
        this.productCode = productCode;
        this.subProductCode = subProductCode;
    }
}
