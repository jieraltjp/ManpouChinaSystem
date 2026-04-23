package com.manpou.allinone.procurement.application.listener;

import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.qc.domain.event.QcRecordCompletedEvent;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcType;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

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
    private final ProductRepository productRepository;
    private final QcRecordRepository qcRecordRepository;

    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onQcPassed(QcRecordCompletedEvent evt) {
        advanceProcurementStatus(evt);
    }

    public void advanceProcurementStatus(QcRecordCompletedEvent evt) {
        if (evt.getProcurementId() == null) {
            return;
        }
        try {
            // 查询真实 QcRecord 获取 qcType（空运推荐的关键依据）
            QcRecord qcRecord = qcRecordRepository.findByIdAndIsDeletedFalse(evt.getQcRecordId()).orElse(null);
            if (qcRecord == null) {
                log.warn("[Procurement] QC record not found for event, qcId={}", evt.getQcRecordId());
                return;
            }
            Procurement procurement = procurementRepository
                    .findByIdAndIsDeletedFalse(evt.getProcurementId())
                    .orElse(null);
            if (procurement == null) {
                log.info("[Procurement] QC passed but procurement not found, procurementId={}",
                        evt.getProcurementId());
                return;
            }
            ShipmentStatus current = procurement.getStatus();
            // 仅当处于検品或現地検品时才推进（防止重复推进）
            if (current != ShipmentStatus.検品 && current != ShipmentStatus.現地検品) {
                log.info("[Procurement] QC passed but procurement status={} not in [検品,現地検品], skip, procurementId={}",
                        current, evt.getProcurementId());
                return;
            }
            // 拉取 Product 尺寸用于空运判定
            Product product = productRepository
                    .findByMasterCodeAndIsDeletedFalse(evt.getProductCode())
                    .orElse(null);
            ShipmentStatus suggested = procurement.suggestNextStatus(qcRecord, product);
            procurement.updateStatus(suggested);
            procurementRepository.save(procurement);
            log.info("[Procurement] QC passed → auto-advanced, procurementId={}, {}→{}, qcType={}",
                    evt.getProcurementId(), current, suggested,
                    qcRecord.getQcType() != null ? qcRecord.getQcType().name() : "null");
        } catch (Exception e) {
            log.error("[Procurement] Failed to auto-advance on QC pass, procurementId={}: {}",
                    evt.getProcurementId(), e.getMessage(), e);
        }
    }
}
