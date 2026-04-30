package com.manpou.allinone.qc.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 验货通过事件。
 *
 * 触发：QcRecord.result = PASS + status → COMPLETED
 * 副作用：推进关联 Procurement 的状态
 *
 * SPEC-B02 §5：验货通过后根据 QC 类型和商品尺寸推荐下一状态
 * SPEC-B03 §3：验货记录为独立聚合根，通过事件驱动跨域协作
 */
@Getter
@RequiredArgsConstructor
public class QcRecordCompletedEvent {

    private final Long qcRecordId;
    private final String qcCode;
    private final Long procurementId;
    private final Long shipmentBatchId;        // V43新增
    private final String productCode;
    private final String subProductCode;
}
