package com.manpou.allinone.sales.application.listener;

import com.manpou.allinone.customs.domain.event.JapanCustomsClearedEvent;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import com.manpou.allinone.sales.domain.model.SalesStatus;
import com.manpou.allinone.sales.domain.repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * 日本清关完成事件监听器。
 *
 * 触发：JapanCustomsRecord.status = CLEARED（onCommit 后触发）
 * 副作用：自动创建 SalesRecord（步骤8 运营销售）
 *
 * SPEC-B08 §3.1 触发规则：
 *   JapanCustomsRecord.status = CLEARED
 *     → 自动创建 SalesRecord
 *       listingDate = today
 *       initialStock = Procurement.quantity
 *       currentStock = Procurement.quantity
 *       status = LISTED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JapanCustomsClearedEventListener {

    private final ProcurementRepository procurementRepository;
    private final SalesRecordRepository salesRecordRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJapanCustomsCleared(JapanCustomsClearedEvent evt) {
        createSalesRecordFromProcurement(evt);
    }

    public void createSalesRecordFromProcurement(JapanCustomsClearedEvent evt) {
        if (evt.getProcurementId() == null) {
            return;
        }
        try {
            // 检查是否已存在关联的 SalesRecord（幂等）
            var existing = salesRecordRepository.findByProcurementIdAndDeletedIsFalse(evt.getProcurementId());
            if (!existing.isEmpty()) {
                log.info("[SalesRecord] already exists for procurementId={}, skip auto-creation",
                        evt.getProcurementId());
                return;
            }

            // 查询采购单获取商品信息
            Procurement procurement = procurementRepository
                    .findByIdAndDeletedIsFalse(evt.getProcurementId())
                    .orElse(null);
            if (procurement == null) {
                log.warn("[SalesRecord] procurement not found for auto-creation, procurementId={}",
                        evt.getProcurementId());
                return;
            }

            // 创建销售记录
            SalesRecord record = new SalesRecord();
            record.generateRecordCode();
            record.setProcurementId(procurement.getId());
            record.setProductCode(procurement.getProductCode());
            record.setSubProductCode(procurement.getSubProductCode());
            record.setListingDate(LocalDate.now());
            int quantity = (procurement.getQuantity() != null ? procurement.getQuantity() : 0);
            record.setInitialStock(quantity);
            record.setCurrentStock(quantity);
            record.setSalesQuantity(0);
            record.setReturnedQuantity(0);
            record.setReturnRate(java.math.BigDecimal.ZERO);
            record.setStatus(SalesStatus.LISTED);

            SalesRecord saved = salesRecordRepository.save(record);
            log.info("[SalesRecord] auto-created from JapanCustoms cleared, id={}, recordCode={}, procurementId={}",
                    saved.getId(), saved.getRecordCode(), procurement.getId());
        } catch (Exception e) {
            log.error("[SalesRecord] failed to auto-create from JapanCustoms cleared, procurementId={}: {}",
                    evt.getProcurementId(), e.getMessage(), e);
        }
    }
}
