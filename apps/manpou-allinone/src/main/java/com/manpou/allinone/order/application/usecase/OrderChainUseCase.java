package com.manpou.allinone.order.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import com.manpou.allinone.customs.domain.repository.JapanCustomsRepository;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import com.manpou.allinone.finance.domain.repository.TaxRefundRepository;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import com.manpou.allinone.order.application.dto.OrderChainDetailVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.FactoryVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.ReplenishmentDemandVO;
import com.manpou.allinone.order.application.dto.ShipmentBatchVO;
import com.manpou.allinone.order.domain.repository.OrderChainRepository;
import com.manpou.allinone.order.domain.view.OrderChainView;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.procurement.domain.repository.ShipmentBatchRepository;
import com.manpou.allinone.qc.application.assembler.QcRecordAssembler;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import com.manpou.allinone.sales.domain.repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 订单总览 UseCase。
 * 步骤1~2 基础数据来自 v_order_chain_v1 视图；
 * 步骤3~4 多批次/多记录数据通过实际表查询补充。
 */
@Service
@RequiredArgsConstructor
public class OrderChainUseCase {

    private final OrderChainRepository orderChainRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;
    private final ShipmentBatchRepository shipmentBatchRepository;
    private final QcRecordRepository qcRecordRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final FactoryRepository factoryRepository;
    private final QcRecordAssembler qcRecordAssembler;
    private final DomesticCustomsRepository domesticCustomsRepository;
    private final JapanCustomsRepository japanCustomsRepository;
    private final TaxRefundRepository taxRefundRepository;
    private final SalesRecordRepository salesRecordRepository;

    @Transactional(readOnly = true)
    public Page<com.manpou.allinone.order.application.dto.OrderChainVO> getChainList(
            String demandStatus, String keyword, Pageable pageable) {
        PageRequest sortFreeRequest = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        Page<OrderChainView> page = orderChainRepository.findChainList(
                demandStatus, keyword, sortFreeRequest);

        List<com.manpou.allinone.order.application.dto.OrderChainVO> sorted = page.getContent().stream()
                .sorted(Comparator.comparing(OrderChainView::getDemandCreateTime).reversed())
                .map(this::toChainVO)
                .toList();

        return new PageImpl<>(sorted, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderChainDetailVO getChainDetail(Long demandId) {
        // 步骤1：从视图获取基础数据（Demand + 快照 + 步骤2~4单条）
        OrderChainView view = orderChainRepository.findByDemandId(demandId)
                .orElseThrow(() -> BusinessException.notFound("Demand", demandId));

        // 步骤1 VO
        ReplenishmentDemandVO demandVO = ReplenishmentDemandVO.builder()
                .id(view.getDemandId())
                .demandCode(view.getDemandCode())
                .demandType(view.getDemandType())
                .productCode(view.getDemandProductCode())
                .subProductCode(view.getDemandSubProductCode())
                .quantity(view.getDemandQuantity())
                .destination(view.getDemandDestination())
                .japanLead(view.getDemandJapanLead())
                .status(view.getDemandStatus())
                .createTime(view.getDemandCreateTime())
                .build();

        // 步骤2：实际表查询（支持退货/退货原因等动态字段）
        ProcurementVO procurementVO = null;
        FactoryVO factoryVO = null;
        List<ShipmentBatchVO> shipmentBatches = List.of();
        List<QcRecordVO> qcRecords = List.of();
        LogisticsPlanVO logisticsPlanVO = null;

        if (view.getProcurementId() != null) {
            Procurement procurement = procurementRepository.findByIdAndDeletedIsFalse(view.getProcurementId())
                    .orElse(null);
            if (procurement != null) {
                procurementVO = ProcurementVO.builder()
                        .id(procurement.getId())
                        .factoryId(procurement.getFactoryId())
                        .productCode(procurement.getProductCode())
                        .subProductCode(procurement.getSubProductCode())
                        .material(procurement.getMaterial())
                        .requiresQc(procurement.getRequiresQc())
                        .quantity(procurement.getQuantity())
                        .priceRmb(procurement.getPriceRmb())
                        .exchangeRate(procurement.getExchangeRate())
                        .taxPoint(procurement.getTaxPoint())
                        .billingType(procurement.getBillingType() != null
                                ? procurement.getBillingType().name() : null)
                        .estimatedPriceJpy(procurement.getEstimatedPriceJpy())
                        .orderDate(procurement.getOrderDate())
                        .factoryShipDate(procurement.getFactoryShipDate())
                        .plannedShipDate(procurement.getPlannedShipDate())
                        .actualShipDate(procurement.getActualShipDate())
                        .productLead(procurement.getProductLead())
                        .japanLead(procurement.getJapanLead())
                        .chinaLead(procurement.getChinaLead())
                        .destination(procurement.getDestination())
                        .customerCompany(procurement.getCustomerCompany())
                        .status(procurement.getStatus() != null
                                ? procurement.getStatus().name() : null)
                        .returnReason(procurement.getReturnReason())
                        .createTime(procurement.getCreateTime())
                        .build();

                // 工厂快照（来自 procurement_snapshot 或 factory 表）
                if (view.getSnapshotFactoryId() != null) {
                    factoryVO = factoryRepository.findByIdAndDeletedIsFalse(view.getSnapshotFactoryId())
                            .map(f -> FactoryVO.builder()
                                    .id(f.getId())
                                    .factoryCode(f.getFactoryCode())
                                    .factoryName(f.getFactoryName())
                                    .province(f.getProvince())
                                    .city(f.getCity())
                                    .contactName(f.getContactName())
                                    .contactPhone(f.getContactPhone())
                                    .build())
                            .orElse(null);
                }

                // 步骤3：厂家出货批次（多批次）
                List<ShipmentBatch> batches = shipmentBatchRepository
                        .findByProcurementIdAndDeletedIsFalse(procurement.getId());
                if (!batches.isEmpty()) {
                    List<ShipmentBatchVO> batchVOs = batches.stream()
                            .sorted(Comparator.comparing(
                                    ShipmentBatch::getFactoryShipDate,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(batch -> {
                                List<QcRecord> batchQcs = qcRecordRepository
                                        .findByShipmentBatchIdAndDeletedIsFalse(batch.getId());
                                int totalPassed = batchQcs.stream()
                                        .mapToInt(q -> q.getPassedCount() != null ? q.getPassedCount() : 0)
                                        .sum();
                                return ShipmentBatchVO.builder()
                                        .id(batch.getId())
                                        .batchCode(batch.getBatchCode())
                                        .procurementId(batch.getProcurementId())
                                        .shipmentQuantity(batch.getShipmentQuantity())
                                        .factoryShipDate(batch.getFactoryShipDate())
                                        .actualShipDate(batch.getActualShipDate())
                                        .status(batch.getStatus())
                                        .remarks(batch.getRemarks())
                                        .qcRecordCount(batchQcs.size())
                                        .totalPassedCount(totalPassed)
                                        .build();
                            })
                            .toList();
                    shipmentBatches = batchVOs;
                }

                // 步骤4：验货记录（多记录，按采购单聚合）
                List<QcRecord> qcs = qcRecordRepository
                        .findByProcurementIdAndDeletedIsFalse(procurement.getId());
                if (!qcs.isEmpty()) {
                    qcRecords = qcs.stream()
                            .sorted(Comparator.comparing(
                                    QcRecord::getQcDate,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(qcRecordAssembler::toDto)
                            .map(qp -> QcRecordVO.builder()
                                    .id(qp.getId())
                                    .qcCode(qp.getQcCode())
                                    .procurementId(qp.getProcurementId())
                                    .productCode(qp.getProductCode())
                                    .subProductCode(qp.getSubProductCode())
                                    .result(qp.getResult() != null ? qp.getResult().name() : null)
                                    .inspectionCount(qp.getInspectionCount())
                                    .passedCount(qp.getPassedCount())
                                    .defectiveCount(qp.getDefectiveCount())
                                    .boxCount(qp.getBoxCount())
                                    .boxLengthCm(qp.getBoxLengthCm())
                                    .boxWidthCm(qp.getBoxWidthCm())
                                    .boxHeightCm(qp.getBoxHeightCm())
                                    .netWeightPerUnit(qp.getNetWeightPerUnit())
                                    .grossWeight(qp.getGrossWeight())
                                    .qcDate(qp.getQcDate())
                                    .qcUserId(qp.getQcUserId())
                                    .status(qp.getStatus() != null ? qp.getStatus().name() : null)
                                    .productImageUrl(qp.getProductImageUrl())
                                    .build())
                            .toList();
                }

                // 步骤5：调配计划（多条，取第一条）
                // 优先按 procurement_id 查；查不到则通过 qc_record_id 链式追溯
                List<com.manpou.allinone.logistics.domain.model.LogisticsPlan> plans =
                        logisticsPlanRepository.findByProcurementIdAndDeletedIsFalse(procurement.getId());
                if (plans.isEmpty() && !qcRecords.isEmpty()) {
                    // 链式追溯：通过第一个验货记录的 id 找调配计划
                    Long firstQcId = qcRecords.get(0).getId();
                    var byQc = logisticsPlanRepository.findByQcRecordIdAndDeletedIsFalse(firstQcId, PageRequest.of(0, 1));
                    if (!byQc.isEmpty()) plans = byQc.getContent();
                }
                if (!plans.isEmpty()) {
                    var plan = plans.get(0);
                    logisticsPlanVO = LogisticsPlanVO.builder()
                            .id(plan.getId())
                            .planCode(plan.getPlanCode())
                            .procurementId(plan.getProcurementId())
                            .factoryId(plan.getFactoryId())
                            .productCode(plan.getProductCode())
                            .subProductCode(plan.getSubProductCode())
                            .planType(plan.getPlanType() != null ? plan.getPlanType().name() : null)
                            .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                            .cargoLengthCm(plan.getCargoLengthCm())
                            .cargoWidthCm(plan.getCargoWidthCm())
                            .cargoHeightCm(plan.getCargoHeightCm())
                            .cargoVolumeCbm(plan.getCargoVolumeCbm())
                            .cargoWeightKg(plan.getCargoWeightKg())
                            .netWeightKg(plan.getNetWeightKg())    // V49
                            .grossWeightKg(plan.getGrossWeightKg())  // V49
                            .quantity(plan.getQuantity())
                            .requiresQc(plan.getRequiresQc())
                            .estimatedShipDate(plan.getEstimatedShipDate())
                            .actualShipDate(plan.getActualShipDate())
                            .build();
                }
            }
        }

        // 步骤6：国内报关
        // 优先按 procurement_id 查；查不到则通过 logistics_plan_id 链式追溯
        OrderOverviewPageVO.DomesticCustomsVO domesticCustomsVO = null;
        var domesticOpt = domesticCustomsRepository.findByProcurementIdAndDeletedIsFalse(view.getProcurementId());
        if (domesticOpt.isEmpty() && logisticsPlanVO != null) {
            domesticOpt = domesticCustomsRepository.findByLogisticsPlanIdAndDeletedIsFalse(logisticsPlanVO.getId())
                    .map(List::of).orElse(List.of());
        }
        if (!domesticOpt.isEmpty()) {
            DomesticCustomsRecord dc = domesticOpt.get(0);
            domesticCustomsVO = OrderOverviewPageVO.DomesticCustomsVO.builder()
                    .id(dc.getId())
                    .customsCode(dc.getCustomsCode())
                    .containerNo(dc.getContainerNo())
                    .procurementId(dc.getProcurementId())
                    .productCode(dc.getProductCode())
                    .status(dc.getStatus() != null ? dc.getStatus().name() : null)
                    .estimatedValueCny(dc.getEstimatedValueCny())
                    .createTime(dc.getCreateTime())
                    .remarks(dc.getRemarks())
                    .build();
        }

        // 步骤7：日本清关
        // 优先按 procurement_id 查；查不到则通过 domesticCustomsId 链式追溯
        OrderOverviewPageVO.JapanCustomsVO japanCustomsVO = null;
        var japanOpt = japanCustomsRepository.findByProcurementIdAndDeletedIsFalse(view.getProcurementId());
        if (japanOpt.isEmpty() && domesticCustomsVO != null) {
            japanOpt = japanCustomsRepository.findByDomesticCustomsIdAndDeletedIsFalse(domesticCustomsVO.getId())
                    .map(List::of).orElse(List.of());
        }
        if (!japanOpt.isEmpty()) {
            JapanCustomsRecord jc = japanOpt.get(0);
            japanCustomsVO = OrderOverviewPageVO.JapanCustomsVO.builder()
                    .id(jc.getId())
                    .containerNo(jc.getContainerNo())
                    .domesticCustomsId(jc.getDomesticCustomsId())
                    .logisticsPlanId(jc.getLogisticsPlanId())
                    .procurementId(jc.getProcurementId())
                    .customsEntryNo(jc.getCustomsEntryNo())
                    .status(jc.getStatus() != null ? jc.getStatus().name() : null)
                    .arrivalDate(jc.getArrivalDate())
                    .clearanceDate(jc.getClearanceDate())
                    .customsBroker(jc.getCustomsBroker())
                    .brokerPhone(jc.getBrokerPhone())
                    .brokerContact(jc.getBrokerContact())
                    .importDutyPaid(jc.getImportDutyPaid())
                    .consumptionTaxPaid(jc.getConsumptionTaxPaid())
                    .arrivalPort(jc.getArrivalPort())
                    .declaredWeightKg(jc.getDeclaredWeightKg())
                    .declaredVolumeCbm(jc.getDeclaredVolumeCbm())
                    .remarks(jc.getRemarks())
                    .build();
        }

        // 步骤8：退税
        // 优先按 procurement_id 查；查不到则通过 japanCustomsId 链式追溯
        OrderOverviewPageVO.TaxRefundVO taxRefundVO = null;
        var taxOpt = taxRefundRepository.findByProcurementIdAndDeletedIsFalse(view.getProcurementId());
        if (taxOpt.isEmpty() && japanCustomsVO != null) {
            taxOpt = taxRefundRepository.findByJapanCustomsIdAndDeletedIsFalse(japanCustomsVO.getId())
                    .map(List::of).orElse(List.of());
        }
        if (!taxOpt.isEmpty()) {
            TaxRefundRecord tr = taxOpt.get(0);
            taxRefundVO = OrderOverviewPageVO.TaxRefundVO.builder()
                    .id(tr.getId())
                    .procurementId(tr.getProcurementId())
                    .japanCustomsId(tr.getJapanCustomsId())
                    .refundCode(tr.getRefundCode())
                    .status(tr.getStatus() != null ? tr.getStatus().name() : null)
                    .billingType(tr.getBillingType() != null ? tr.getBillingType().name() : null)
                    .priceRmb(tr.getPriceRmb())
                    .quantity(tr.getQuantity())
                    .taxPoint(tr.getTaxPoint())
                    .estimatedRefundRmb(tr.getEstimatedRefundRmb())
                    .actualRefundRmb(tr.getActualRefundRmb())
                    .exchangeRate(tr.getExchangeRate())
                    .refundDate(tr.getRefundDate())
                    .refundBank(tr.getRefundBank())
                    .remarks(tr.getRemarks())
                    .build();
        }

        // 步骤9：运营销售（仅按 procurement_id 查）
        OrderOverviewPageVO.SalesRecordVO salesRecordVO = null;
        List<SalesRecord> salesList =
                salesRecordRepository.findByProcurementIdAndDeletedIsFalse(view.getProcurementId());
        if (!salesList.isEmpty()) {
            SalesRecord sr = salesList.get(0);
            salesRecordVO = OrderOverviewPageVO.SalesRecordVO.builder()
                    .id(sr.getId())
                    .recordCode(sr.getRecordCode())
                    .procurementId(sr.getProcurementId())
                    .productCode(sr.getProductCode())
                    .subProductCode(sr.getSubProductCode())
                    .salesChannel(sr.getSalesChannel() != null ? sr.getSalesChannel().name() : null)
                    .listingDate(sr.getListingDate())
                    .initialStock(sr.getInitialStock())
                    .currentStock(sr.getCurrentStock())
                    .safetyStock(sr.getSafetyStock())
                    .salesQuantity(sr.getSalesQuantity())
                    .returnedQuantity(sr.getReturnedQuantity())
                    .returnRate(sr.getReturnRate())
                    .sellingPriceJpy(sr.getSellingPriceJpy())
                    .status(sr.getStatus() != null ? sr.getStatus().name() : null)
                    .remarks(sr.getRemarks())
                    .build();
        }

        return OrderChainDetailVO.builder()
                .demandId(view.getDemandId())
                .demand(demandVO)
                .procurement(procurementVO)
                .factory(factoryVO)
                .shipmentBatches(shipmentBatches.isEmpty() ? null : shipmentBatches)
                .qcRecords(qcRecords.isEmpty() ? null : qcRecords)
                .logisticsPlan(logisticsPlanVO)
                .domesticCustoms(domesticCustomsVO)
                .japanCustoms(japanCustomsVO)
                .taxRefund(taxRefundVO)
                .salesRecord(salesRecordVO)
                .build();
    }

    /**
     * 级联软删除 Demand 及其关联的全链路数据（步骤1~5）。
     * 删除顺序：QcRecord → ShipmentBatch → LogisticsPlan → Procurement → Demand。
     */
    @Transactional
    public void deleteChain(Long demandId) {
        ReplenishmentDemand demand = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("Demand", demandId));

        Long procurementId = demand.getLinkedProcurementId();

        if (procurementId != null) {
            // 步骤4：软删除验货记录（按采购单 + 按批次）
            List<QcRecord> qcByProcurement = qcRecordRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
            for (QcRecord qc : qcByProcurement) {
                qc.markDeleted();
                qcRecordRepository.save(qc);
            }

            // 步骤3：软删除出货批次
            List<ShipmentBatch> batches = shipmentBatchRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
            for (ShipmentBatch batch : batches) {
                batch.markDeleted();
                shipmentBatchRepository.save(batch);
            }

            // 步骤5：软删除调配计划
            List<LogisticsPlan> logisticsPlans = logisticsPlanRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
            for (LogisticsPlan lp : logisticsPlans) {
                lp.markDeleted();
                logisticsPlanRepository.save(lp);
            }

            // 步骤2：软删除采购单
            procurementRepository.findByIdAndDeletedIsFalse(procurementId)
                    .ifPresent(p -> {
                        p.markDeleted();
                        procurementRepository.save(p);
                    });

            // 解除 Demand → Procurement 关联
            demand.setLinkedProcurementId(null);
        }

        // 步骤1：软删除需求
        demand.markDeleted();
        demandRepository.save(demand);
    }

    private com.manpou.allinone.order.application.dto.OrderChainVO toChainVO(OrderChainView v) {
        com.manpou.allinone.order.application.dto.OrderChainVO.SnapshotVO snapshotVO = null;
        if (v.getSnapshotFactoryName() != null || v.getSnapshotProductNameZh() != null) {
            snapshotVO = com.manpou.allinone.order.application.dto.OrderChainVO.SnapshotVO.builder()
                    .factoryId(v.getSnapshotFactoryId())
                    .factoryCode(v.getSnapshotFactoryCode())
                    .factoryName(v.getSnapshotFactoryName())
                    .factoryProvince(v.getSnapshotFactoryProvince())
                    .factoryCity(v.getSnapshotFactoryCity())
                    .factoryContactName(v.getSnapshotFactoryContactName())
                    .factoryContactPhone(v.getSnapshotFactoryContactPhone())
                    .productNameZh(v.getSnapshotProductNameZh())
                    .productNameJa(v.getSnapshotProductNameJa())
                    .productCategory(v.getSnapshotProductCategory())
                    .build();
        }

        return com.manpou.allinone.order.application.dto.OrderChainVO.builder()
                .demandId(v.getDemandId())
                .demandCode(v.getDemandCode())
                .demandType(v.getDemandType())
                .demandProductCode(v.getDemandProductCode())
                .demandSubProductCode(v.getDemandSubProductCode())
                .demandQuantity(v.getDemandQuantity())
                .demandDestination(v.getDemandDestination())
                .demandJapanLead(v.getDemandJapanLead())
                .demandStatus(v.getDemandStatus())
                .linkedProcurementId(v.getLinkedProcurementId())
                .demandImageUrl(v.getDemandImageUrl())
                .demandCreateTime(v.getDemandCreateTime())
                .demandUpdateTime(v.getDemandUpdateTime())
                .snapshot(snapshotVO)
                .step1Status(v.getStep1Status())
                .step2Status(v.getStep2Status())
                .step3Status(v.getStep3Status())
                .step4Status(v.getStep4Status())
                .step5Status(v.getStep5Status())
                .step6Status(v.getStep6Status())
                .step7Status(v.getStep7Status())
                .step8Status(v.getStep8Status())
                .step9Status(v.getStep9Status())
                .procurementPriceRmb(v.getProcurementPriceRmb() != null
                        ? v.getProcurementPriceRmb().toPlainString() : null)
                .procurementTaxPoint(v.getProcurementTaxPoint() != null
                        ? v.getProcurementTaxPoint().toPlainString() : null)
                .procurementBillingType(v.getProcurementBillingType())
                .procurementCreateBy(v.getProcurementCreateBy())
                .build();
    }
}
