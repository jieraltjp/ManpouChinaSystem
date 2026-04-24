package com.manpou.allinone.order.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import com.manpou.allinone.customs.domain.repository.JapanCustomsRepository;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import com.manpou.allinone.finance.domain.repository.TaxRefundRepository;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import com.manpou.allinone.sales.domain.repository.SalesRecordRepository;
import com.manpou.allinone.order.application.assembler.OrderOverviewAssembler;
import com.manpou.allinone.order.application.dto.OrderDemandSelectorDTO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.DemandVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.DomesticCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.JapanCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.SalesRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.StepStatus;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.TaxRefundVO;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单总览用例。
 * 以 Procurement.id 为锚点，聚合全链路 8 步数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOverviewUseCase {

    private final ProcurementRepository procurementRepository;
    private final FactoryRepository factoryRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final QcRecordRepository qcRecordRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final DomesticCustomsRepository domesticCustomsRepository;
    private final JapanCustomsRepository japanCustomsRepository;
    private final TaxRefundRepository taxRefundRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final OrderOverviewAssembler assembler;

    @Transactional(readOnly = true)
    public OrderOverviewPageVO getOverview(Long procurementId) {
        Procurement procurement = procurementRepository
                .findByIdAndDeletedIsFalse(procurementId)
                .orElseThrow(() -> BusinessException.notFound("Procurement", procurementId));

        log.info("[OrderOverview] fetch, procurementId={}", procurementId);

        // 锚点发注单
        String factoryName = procurement.getFactoryId() != null
                ? factoryRepository.findByIdAndDeletedIsFalse(procurement.getFactoryId())
                        .map(Factory::getFactoryName).orElse(null)
                : null;
        ProcurementVO procurementVO = assembler.toProcurementVO(procurement, factoryName);

        // 步骤1：补货需求（通过 Procurement.linkedDemandId 关联，v1.6.0）
        DemandVO demandVO = null;
        if (procurement.getLinkedDemandId() != null) {
            demandVO = demandRepository.findByIdAndDeletedIsFalse(procurement.getLinkedDemandId())
                    .map(assembler::toDemandVO).orElse(null);
        }

        // 步骤3：验货记录（取最新一条）
        QcRecordVO qcVO = qcRecordRepository
                .findByProcurementIdAndDeletedIsFalse(procurementId, Pageable.ofSize(1))
                .stream().findFirst()
                .map(assembler::toQcVO).orElse(null);

        // 步骤4：调配计划（取最新一条）
        LogisticsPlanVO lpVO = logisticsPlanRepository
                .findByProcurementIdAndDeletedIsFalse(procurementId, Pageable.ofSize(1))
                .stream().findFirst()
                .map(assembler::toLogisticsVO).orElse(null);

        // 步骤5：国内报关（取最新一条）
        DomesticCustomsVO dcVO = null;
        List<DomesticCustomsRecord> dcRecords = domesticCustomsRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
        if (!dcRecords.isEmpty()) {
            dcVO = assembler.toDomesticCustomsVO(dcRecords.get(0));
        }

        // 步骤6：日本清关（通过 procurementId 关联，取最新一条）
        JapanCustomsVO jpVO = null;
        List<JapanCustomsRecord> jpRecords = japanCustomsRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
        if (!jpRecords.isEmpty()) {
            jpVO = assembler.toJapanCustomsVO(jpRecords.get(0));
        }

        // 步骤7：出口退税（通过 procurementId 关联，取最新一条）
        TaxRefundVO trVO = null;
        List<TaxRefundRecord> trRecords = taxRefundRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
        if (!trRecords.isEmpty()) {
            trVO = assembler.toTaxRefundVO(trRecords.get(0));
        }

        // 步骤8：运营销售（通过 procurementId 关联，取最新一条）
        SalesRecordVO srVO = null;
        List<SalesRecord> srRecords = salesRecordRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
        if (!srRecords.isEmpty()) {
            srVO = assembler.toSalesRecordVO(srRecords.get(0));
        }

        // 计算8步状态
        StepStatus[] stepStatuses = assembler.computeStepStatuses(
                procurement, demandVO, qcVO, lpVO, dcVO, jpVO, trVO, srVO);

        return OrderOverviewPageVO.builder()
                .procurementId(procurementId)
                .procurement(procurementVO)
                .factory(procurement.getFactoryId() != null
                        ? factoryRepository.findByIdAndDeletedIsFalse(procurement.getFactoryId())
                                .map(assembler::toFactoryVO).orElse(null)
                        : null)
                .demand(demandVO)
                .qcRecord(qcVO)
                .logisticsPlan(lpVO)
                .domesticCustoms(dcVO)
                .japanCustoms(jpVO)
                .taxRefund(trVO)
                .salesRecord(srVO)
                .stepStatuses(stepStatuses)
                .build();
    }

    // ===== Demand 锚点方法（新增）=====

    /**
     * Demand 选择器 DTO 转换（供 Controller 直接调用）。
     */
    public OrderDemandSelectorDTO toDemandSelectorDto(ReplenishmentDemand entity) {
        String summary = buildSubProductItemsSummary(entity.getSubProductItemsRaw());
        return OrderDemandSelectorDTO.builder()
                .id(entity.getId())
                .demandCode(entity.getDemandCode())
                .demandType(entity.getDemandType() != null ? entity.getDemandType().name() : null)
                .productCode(entity.getProductCode())
                .subProductItemsSummary(summary)
                .japanLead(entity.getJapanLead())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * 以 Demand 为锚点的订单总览。
     * 用于 /api/v1/orders/demands/{demandId}/overview
     */
    @Transactional(readOnly = true)
    public OrderOverviewPageVO getDemandOverview(Long demandId) {
        ReplenishmentDemand demand = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));

        log.info("[OrderOverview] getDemandOverview, demandId={}", demandId);

        DemandVO demandVO = assembler.toDemandVO(demand);

        // Demand 锚点：step1=COMPLETED，step2-8=NOT_STARTED
        StepStatus[] stepStatuses = assembler.computeDemandStepStatuses();

        return OrderOverviewPageVO.builder()
                .demand(demandVO)
                .stepStatuses(stepStatuses)
                .build();
    }

    private String buildSubProductItemsSummary(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            var items = assembler.parseSubProductItemsForDemand(raw);
            if (items == null || items.isEmpty()) return null;
            return items.stream()
                    .map(item -> {
                        String qty = item.getQuantity() != null ? String.valueOf(item.getQuantity()) : "?";
                        String dest = item.getDestination() != null ? item.getDestination() : "";
                        return item.getSubCode() + ":" + qty + (dest.isEmpty() ? "" : dest);
                    })
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(null);
        } catch (Exception e) {
            return raw;
        }
    }
}
