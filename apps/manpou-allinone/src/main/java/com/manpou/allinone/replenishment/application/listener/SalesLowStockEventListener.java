package com.manpou.allinone.replenishment.application.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.application.assembler.ReplenishmentDemandAssembler;
import com.manpou.allinone.replenishment.application.dto.SubProductItemDto;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesLowStockEventListener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static int SEQ = (int) (System.currentTimeMillis() % 1000);

    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;
    private final ReplenishmentDemandAssembler assembler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(ReplenishmentDemandNeededEvent evt) {
        createReplenishmentDemand(evt);
    }

    public void createReplenishmentDemand(ReplenishmentDemandNeededEvent evt) {
        if (evt.getProductCode() == null) return;
        try {
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

            String demandCode = generateDemandCode();

            ReplenishmentDemand demand = new ReplenishmentDemand();
            demand.setDemandCode(demandCode);
            demand.setDemandType(DemandType.REPLENISHMENT);
            demand.setProductCode(evt.getProductCode());
            SubProductItem item = new SubProductItem(
                    evt.getSubProductCode(),
                    evt.getRequestedQuantity(),
                    destination
            );
            demand.setSubProductItemsRaw(assembler.serializeSubProductItems(
                    List.of(new SubProductItemDto(evt.getSubProductCode(), evt.getRequestedQuantity(), destination))));
            demand.setJapanLead(japanLead);
            demand.setRemarks(String.format(
                    "【自动生成】库存不足，当前库存 %d，安全库存 %d，补货数量 %d。关联销售记录 ID: %d",
                    evt.getCurrentStock(), evt.getSafetyStock(), evt.getRequestedQuantity(), evt.getSalesRecordId()));

            ReplenishmentDemand saved = demandRepository.save(demand);
            log.info("[ReplenishmentDemand] auto-created from low stock, traceId={}, id={}, demandCode={}, quantity={}, salesRecordId={}",
                    MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(),
                    evt.getRequestedQuantity(), evt.getSalesRecordId());
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
