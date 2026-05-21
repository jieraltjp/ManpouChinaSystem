package com.manpou.allinone.procurement.application.listener;

import com.manpou.allinone.common.port.ProductQueryPort;
import com.manpou.allinone.common.port.QcQueryPort;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.procurement.domain.repository.ShipmentBatchRepository;
import com.manpou.allinone.qc.domain.event.QcRecordCompletedEvent;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * 验货通过事件监听器。
 *
 * 触发：QcRecord.result = PASS
 * 副作用：自动推进关联 Procurement 状态至検品之后的下一步
 *
 * SPEC-B02 §5 onQcPassed() 规则：
 *   - 現地検品(REMOTE) → メーカー直送
 *   - 倉庫検品(ONSITE) + 空运条件 → エア便（轻小件推荐空运）
 *   - 倉庫検品(ONSITE) + 不达标 → 輸出（海运）
 *
 * 注意：此监听器仅推进处于「検品」或「現地検品」状态的订单。
 * 如果订单已推进到后续状态则跳过（幂等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcurementQcPassedEventListener {

    private final ProcurementRepository procurementRepository;
    private final ShipmentBatchRepository shipmentBatchRepository;
    private final ProductQueryPort productQueryPort;
    private final QcQueryPort qcQueryPort;

    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onQcPassed(QcRecordCompletedEvent evt) {
        advanceProcurementStatus(evt);
    }

    public void advanceProcurementStatus(QcRecordCompletedEvent evt) {
        // V43后：优先用 evt.procurementId；若无，从 qcRecord.shipmentBatchId 反查
        Long procurementId = evt.getProcurementId();
        if (procurementId == null) {
            QcRecord qcRecord = qcQueryPort.findById(evt.getQcRecordId()).orElse(null);
            if (qcRecord != null && qcRecord.getShipmentBatchId() != null) {
                ShipmentBatch batch = shipmentBatchRepository
                        .findByIdAndDeletedIsFalse(qcRecord.getShipmentBatchId())
                        .orElse(null);
                if (batch != null) {
                    procurementId = batch.getProcurementId();
                }
            }
        }
        if (procurementId == null) {
            log.info("[Procurement] QC passed but no linked procurement, skip, qcId={}", evt.getQcRecordId());
            return;
        }
        try {
            QcRecord qcRecord = qcQueryPort.findById(evt.getQcRecordId()).orElse(null);
            if (qcRecord == null) {
                log.warn("[Procurement] QC record not found for event, qcId={}", evt.getQcRecordId());
                return;
            }
            Procurement procurement = procurementRepository
                    .findByIdAndDeletedIsFalse(procurementId)
                    .orElse(null);
            if (procurement == null) {
                log.info("[Procurement] QC passed but procurement not found, procurementId={}",
                        procurementId);
                return;
            }
            ShipmentStatus current = procurement.getStatus();
            if (current != ShipmentStatus.検品 && current != ShipmentStatus.現地検品
                    && current != ShipmentStatus.已出货) {
                log.info("[Procurement] QC passed but procurement status={} not in [検品,現地検品,已出货], skip, procurementId={}",
                        current, procurementId);
                return;
            }
            var product = productQueryPort.findByMasterCode(evt.getProductCode()).orElse(null);
            ShipmentStatus suggested = resolveSuggestedStatus(current, qcRecord, product);
            procurement.updateStatus(suggested, null);
            procurementRepository.save(procurement);
            log.info("[Procurement] QC passed → auto-advanced, procurementId={}, {}→{}, qcType={}",
                    procurementId, current, suggested,
                    qcRecord.getQcType() != null ? qcRecord.getQcType().name() : "null");
        } catch (Exception e) {
            log.error("[Procurement] Failed to auto-advance on QC pass, procurementId={}: {}",
                    procurementId, e.getMessage(), e);
        }
    }

    /**
     * 根据 QC 类型和产品体积判断下一步状态。
     * 规则（SPEC-B02 §5）：
     *   - 現地検品(REMOTE) → メーカー直送
     *   - 倉庫検品(ONSITE) + 体积≤0.5m³ → エア便（轻小件推荐空运）
     *   - 倉庫検品(ONSITE) + 体积>0.5m³ → 輸出（海运）
     */
    private ShipmentStatus resolveSuggestedStatus(ShipmentStatus current, QcRecord qcRecord, Product product) {
        if (current == ShipmentStatus.現地検品) {
            return ShipmentStatus.メーカー直送;
        }
        if (qcRecord.getQcType() == QcType.ONSITE) {
            if (product != null && product.getVolumeCbm() != null
                    && product.getVolumeCbm().compareTo(new BigDecimal("0.5")) <= 0) {
                return ShipmentStatus.エア便;
            }
            return ShipmentStatus.輸出;
        }
        return ShipmentStatus.輸出;
    }
}
