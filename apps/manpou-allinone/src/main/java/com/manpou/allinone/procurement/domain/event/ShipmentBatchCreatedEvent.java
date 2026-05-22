package com.manpou.allinone.procurement.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 出货批次创建事件（SPEC-B13）。
 *
 * 触发：ShipmentBatchUseCase.create()
 * 副作用：QcRecordAutoExemptListener 接收后，若工厂 needsQc=false，
 *         自动创建一条 COMPLETED 状态的免验验货记录
 */
@Getter
@RequiredArgsConstructor
public class ShipmentBatchCreatedEvent {

    private final Long batchId;
    private final Long procurementId;
    private final Long factoryId;
}
