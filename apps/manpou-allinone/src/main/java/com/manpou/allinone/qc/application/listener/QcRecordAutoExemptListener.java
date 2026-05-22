package com.manpou.allinone.qc.application.listener;

import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.procurement.domain.event.ShipmentBatchCreatedEvent;
import com.manpou.allinone.qc.application.assembler.QcRecordAssembler;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.model.QcType;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 出货批次创建时，自动创建免验验货记录（SPEC-B13）。
 *
 * 触发：ShipmentBatchUseCase.create() 发布 ShipmentBatchCreatedEvent
 * 条件：工厂 needsQc = false（老厂家）
 * 效果：生成一条 status=COMPLETED, qcType=EXEMPT, result=PASS 的验货记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QcRecordAutoExemptListener {

    private final FactoryRepository factoryRepository;
    private final ProcurementRepository procurementRepository;
    private final QcRecordRepository qcRecordRepository;
    private final QcRecordAssembler qcRecordAssembler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentBatchCreated(ShipmentBatchCreatedEvent event) {
        createExemptQcRecord(event);
    }

    @Transactional
    public void createExemptQcRecord(ShipmentBatchCreatedEvent event) {
        if (event.getFactoryId() == null) {
            log.debug("[QcRecord] exempt skip: factoryId is null, batchId={}", event.getBatchId());
            return;
        }
        try {
            Factory factory = factoryRepository.findByIdAndDeletedIsFalse(event.getFactoryId()).orElse(null);
            if (factory == null) {
                log.warn("[QcRecord] exempt skip: factory not found, factoryId={}", event.getFactoryId());
                return;
            }
            if (factory.getNeedsQc() == null || factory.getNeedsQc()) {
                log.debug("[QcRecord] exempt skip: factory needs QC, factoryId={}", event.getFactoryId());
                return;
            }

            // 幂等：检查是否已有验货记录
            var existing = qcRecordRepository.findByShipmentBatchIdAndDeletedIsFalse(event.getBatchId(), PageRequest.of(0, 1));
            if (existing.hasContent()) {
                log.info("[QcRecord] exempt skip: QC record already exists for batchId={}", event.getBatchId());
                return;
            }

            // 获取 productCode
            String productCode = null;
            String subProductCode = null;
            String sellerName = factory.getFactoryName();

            if (event.getProcurementId() != null) {
                Procurement procurement = procurementRepository.findByIdAndDeletedIsFalse(event.getProcurementId()).orElse(null);
                if (procurement != null) {
                    productCode = procurement.getProductCode();
                    subProductCode = procurement.getSubProductCode();
                }
            }

            // 构建免验验货记录
            QcRecord entity = new QcRecord();
            entity.setQcCode(qcRecordAssembler.generateQcCode());
            entity.setShipmentBatchId(event.getBatchId());
            entity.setProcurementId(event.getProcurementId());
            entity.setSellerName(sellerName);
            entity.setProductCode(productCode != null ? productCode : "UNKNOWN");
            entity.setSubProductCode(subProductCode);
            entity.setQcType(QcType.EXEMPT);
            entity.setResult(QcResult.PASS);
            entity.setStatus(QcStatus.COMPLETED);
            entity.setRemarks("老厂家免验");
            qcRecordRepository.save(entity);
            log.info("[QcRecord] exempt created, batchId={}, qcCode={}, factoryId={}",
                    event.getBatchId(), entity.getQcCode(), event.getFactoryId());
        } catch (Exception e) {
            log.error("[QcRecord] failed to create exempt QC record, batchId={}: {}",
                    event.getBatchId(), e.getMessage(), e);
        }
    }
}
