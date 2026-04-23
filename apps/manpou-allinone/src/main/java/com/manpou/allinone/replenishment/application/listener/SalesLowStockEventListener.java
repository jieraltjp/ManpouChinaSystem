package com.manpou.allinone.replenishment.application.listener;

import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import com.manpou.allinone.sales.domain.event.ReplenishmentDemandNeededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 库存不足事件监听器。
 *
 * 触发：SalesRecord.currentStock < SalesRecord.safetyStock（onCommit 后触发）
 * 副作用：自动创建 ReplenishmentDemand（步骤1）
 *
 * SPEC-B08 §4.1 反馈循环规则：
 *   SalesRecord.isLowStock() = true
 *     → generateReplenishment() → ReplenishmentDemand
 *       demandType = REPLENISHMENT
 *       productCode = this.productCode
 *       subProductCode = this.subProductCode
 *       quantity = safetyThreshold - currentStock
 *       destination = this.destination（来自 Procurement）
 *       japanLead = this.japanLead（来自 Procurement）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesLowStockEventListener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static long SEQ = System.currentTimeMillis() % 1000;

    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(ReplenishmentDemandNeededEvent evt) {
        createReplenishmentDemand(evt);
    }

    public void createReplenishmentDemand(ReplenishmentDemandNeededEvent evt) {
        if (evt.getProductCode() == null) {
            return;
        }
        try {
            // 查询采购单获取目的地和日本担当
            String destination = null;
            String japanLead = null;
            if (evt.getProcurementId() != null) {
                Procurement procurement = procurementRepository
                        .findByIdAndDeletedIsFalse(evt.getProcurementId())
                        .orElse(null);
                if (procurement != null) {
                    destination = procurement.getDestination();
                    japanLead = procurement.getJapanLead();
                }
            }

            // 生成需求编号
            String demandCode = generateDemandCode();

            // 创建补货需求
            ReplenishmentDemand demand = new ReplenishmentDemand();
            demand.setDemandCode(demandCode);
            demand.setDemandType(DemandType.REPLENISHMENT);
            demand.setProductCode(evt.getProductCode());
            demand.setSubProductCode(evt.getSubProductCode());
            demand.setQuantity(evt.getRequestedQuantity());
            demand.setDestination(destination);
            demand.setJapanLead(japanLead);
            demand.setRemarks(String.format(
                    "【自动生成】库存不足，当前库存 %d，安全库存 %d，补货数量 %d。关联销售记录 ID: %d",
                    evt.getCurrentStock(), evt.getSafetyStock(), evt.getRequestedQuantity(), evt.getSalesRecordId()));

            ReplenishmentDemand saved = demandRepository.save(demand);
            log.info("[ReplenishmentDemand] auto-created from low stock, traceId={}, id={}, demandCode={}, quantity={}, salesRecordId={}",
                    MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(),
                    saved.getQuantity(), evt.getSalesRecordId());
        } catch (Exception e) {
            log.error("[ReplenishmentDemand] failed to auto-create from low stock, salesRecordId={}: {}",
                    evt.getSalesRecordId(), e.getMessage(), e);
        }
    }

    private String generateDemandCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("DM-%s-%03d", date, (++SEQ) % 1000);
    }
}
