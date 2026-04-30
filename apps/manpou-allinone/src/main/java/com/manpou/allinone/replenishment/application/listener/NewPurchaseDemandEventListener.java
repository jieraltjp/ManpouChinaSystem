package com.manpou.allinone.replenishment.application.listener;

import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.replenishment.application.assembler.ReplenishmentDemandAssembler;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import com.manpou.allinone.sales.domain.event.NewPurchaseDemandNeededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 新品采购需求事件监听器（NEW_PURCHASE 触发）。
 *
 * 触发条件待产品经理确认，当前为占位实现：
 * - 客户询价记录 → 新品意向
 * - 竞争对手分析 → 新品货号规划
 * - 销售预测 → 爆款提前备货
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewPurchaseDemandEventListener {

    private static final int DEFAULT_NEW_PURCHASE_QTY = 100;

    private final ReplenishmentDemandRepository demandRepository;
    private final ReplenishmentDemandAssembler assembler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewPurchaseDemand(NewPurchaseDemandNeededEvent evt) {
        createNewPurchaseDemand(evt);
    }

    public void createNewPurchaseDemand(NewPurchaseDemandNeededEvent evt) {
        if (evt.getProductCode() == null) return;
        try {
            int quantity = evt.getRequestedQuantity() != null
                    ? evt.getRequestedQuantity()
                    : DEFAULT_NEW_PURCHASE_QTY;

            ReplenishmentDemand demand = new ReplenishmentDemand();
            demand.setDemandCode(assembler.generateDemandCode());
            demand.setDemandType(DemandType.NEW_PURCHASE);
            demand.setProductCode(evt.getProductCode());
            demand.setSubProductCode(evt.getSubProductCode());
            demand.setQuantity(quantity);
            demand.setRemarks(String.format(
                    "【新品采购-自动生成】来源: %s，预估数量: %d",
                    evt.getTriggerSource() != null ? evt.getTriggerSource() : "待确认", quantity));

            ReplenishmentDemand saved = demandRepository.save(demand);
            log.info("[ReplenishmentDemand] auto-created NEW_PURCHASE, traceId={}, id={}, demandCode={}, quantity={}, source={}",
                    MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(),
                    quantity, evt.getTriggerSource());
        } catch (Exception e) {
            log.error("[ReplenishmentDemand] failed to auto-create NEW_PURCHASE, productCode={}: {}",
                    evt.getProductCode(), e.getMessage(), e);
        }
    }
}
