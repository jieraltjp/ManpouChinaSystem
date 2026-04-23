package com.manpou.allinone.logistics.application.listener;

import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import com.manpou.allinone.logistics.domain.event.LogisticsPlanInTransitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 调配计划进入运输中状态的事件监听器。
 *
 * 触发：LogisticsPlan.status → IN_TRANSIT
 * 副作用：自动创建 DomesticCustomsRecord（国内报关记录）
 *
 * SPEC-B04 §5：LogisticsPlan.status = IN_TRANSIT → 自动创建 DomesticCustomsRecord
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticsInTransitEventListener {

    private final DomesticCustomsRepository domesticCustomsRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onInTransit(LogisticsPlanInTransitEvent evt) {
        createDomesticCustomsRecord(evt);
    }

    public void createDomesticCustomsRecord(LogisticsPlanInTransitEvent evt) {
        try {
            // 幂等检查：同一 LogisticsPlan 只创建一条记录
            if (domesticCustomsRepository
                    .findByLogisticsPlanIdAndDeletedIsFalse(evt.getLogisticsPlanId())
                    .isPresent()) {
                log.info("[Customs] DomesticCustomsRecord already exists for logisticsPlanId={}, skip",
                        evt.getLogisticsPlanId());
                return;
            }

            DomesticCustomsRecord record = new DomesticCustomsRecord();
            record.setCustomsCode(generateCustomsCode());
            record.setLogisticsPlanId(evt.getLogisticsPlanId());
            record.setProcurementId(evt.getProcurementId());
            record.setFactoryId(evt.getFactoryId());
            record.setProductCode(evt.getProductCode());
            record.setSubProductCode(evt.getSubProductCode());

            domesticCustomsRepository.save(record);
            log.info("[Customs] DomesticCustomsRecord auto-created, id={}, customsCode={}, "
                            + "triggeredBy logisticsPlanId={}, planCode={}",
                    record.getId(), record.getCustomsCode(),
                    evt.getLogisticsPlanId(), evt.getPlanCode());
        } catch (Exception e) {
            log.error("[Customs] Failed to auto-create DomesticCustomsRecord for logisticsPlanId={}: {}",
                    evt.getLogisticsPlanId(), e.getMessage(), e);
        }
    }

    private String generateCustomsCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("DC-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }
}
