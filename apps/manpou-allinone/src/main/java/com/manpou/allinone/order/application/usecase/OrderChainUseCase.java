package com.manpou.allinone.order.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.order.application.dto.OrderChainDetailVO;

import java.util.Comparator;
import java.util.List;
import com.manpou.allinone.order.application.dto.OrderChainVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.FactoryVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.ReplenishmentDemandVO;
import com.manpou.allinone.order.domain.repository.OrderChainRepository;
import com.manpou.allinone.order.domain.view.OrderChainView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单总览 UseCase（Phase1 步骤1~4）。
 * 基于 MySQL VIEW v_order_chain_v1（使用 procurement_snapshot 快照数据）。
 */
@Service
@RequiredArgsConstructor
public class OrderChainUseCase {

    private final OrderChainRepository orderChainRepository;

    @Transactional(readOnly = true)
    public Page<OrderChainVO> getChainList(String demandStatus, String keyword, Pageable pageable) {
        // TODO(Phase2): In-memory sort + DB分页导致大页码结果与全表排序不一致
        //   正确做法：在 native query 的 ORDER BY 子句中加入 demand_create_time DESC
        //   Phase1 数据量小，暂时接受此限制。
        PageRequest sortFreeRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        Page<OrderChainView> page = orderChainRepository.findChainList(demandStatus, keyword, sortFreeRequest);

        List<OrderChainVO> sorted = page.getContent().stream()
                .sorted(Comparator.comparing(OrderChainView::getDemandCreateTime).reversed())
                .map(this::toChainVO)
                .toList();

        return new PageImpl<>(sorted, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderChainDetailVO getChainDetail(Long demandId) {
        OrderChainView view = orderChainRepository.findByDemandId(demandId)
                .orElseThrow(() -> BusinessException.notFound("Demand", demandId));
        return toDetailVO(view);
    }

    private OrderChainVO toChainVO(OrderChainView v) {
        OrderChainVO.SnapshotVO snapshotVO = null;
        if (v.getSnapshotFactoryName() != null || v.getSnapshotProductNameZh() != null) {
            snapshotVO = OrderChainVO.SnapshotVO.builder()
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

        return OrderChainVO.builder()
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
                .build();
    }

    private OrderChainDetailVO toDetailVO(OrderChainView v) {
        ReplenishmentDemandVO demandVO = ReplenishmentDemandVO.builder()
                .id(v.getDemandId())
                .demandCode(v.getDemandCode())
                .demandType(v.getDemandType())
                .productCode(v.getDemandProductCode())
                .subProductCode(v.getDemandSubProductCode())
                .quantity(v.getDemandQuantity())
                .destination(v.getDemandDestination())
                .japanLead(v.getDemandJapanLead())
                .status(v.getDemandStatus())
                .createTime(v.getDemandCreateTime())
                .build();

        ProcurementVO procurementVO = null;
        FactoryVO factoryVO = null;
        if (v.getProcurementId() != null) {
            procurementVO = ProcurementVO.builder()
                    .id(v.getProcurementId())
                    .procurementCode(v.getProcurementCode())
                    .factoryId(v.getProcurementFactoryId())
                    .productCode(v.getProcurementProductCode())
                    .subProductCode(v.getProcurementSubProductCode())
                    .quantity(v.getProcurementQuantity())
                    .priceRmb(v.getProcurementPriceRmb())
                    .exchangeRate(v.getProcurementExchangeRate())
                    .taxPoint(v.getProcurementTaxPoint())
                    .billingType(v.getProcurementBillingType())
                    .estimatedPriceJpy(v.getProcurementEstimatedPriceJpy())
                    .orderDate(v.getProcurementOrderDate())
                    .factoryShipDate(v.getProcurementFactoryShipDate())
                    .plannedShipDate(v.getProcurementPlannedShipDate())
                    .actualShipDate(v.getProcurementActualShipDate())
                    .productLead(v.getProcurementProductLead())
                    .japanLead(v.getProcurementJapanLead())
                    .chinaLead(v.getProcurementChinaLead())
                    .destination(v.getProcurementDestination())
                    .customerCompany(v.getProcurementCustomerCompany())
                    .status(v.getProcurementStatus())
                    .createTime(v.getProcurementCreateTime())
                    .build();

            factoryVO = FactoryVO.builder()
                    .id(v.getSnapshotFactoryId())
                    .factoryCode(v.getSnapshotFactoryCode())
                    .factoryName(v.getSnapshotFactoryName())
                    .province(v.getSnapshotFactoryProvince())
                    .city(v.getSnapshotFactoryCity())
                    .contactName(v.getSnapshotFactoryContactName())
                    .contactPhone(v.getSnapshotFactoryContactPhone())
                    .build();
        }

        QcRecordVO qcRecordVO = null;
        if (v.getQcRecordId() != null) {
            qcRecordVO = QcRecordVO.builder()
                    .id(v.getQcRecordId())
                    .qcCode(v.getQcCode())
                    .procurementId(v.getProcurementId())
                    .result(v.getQcResult())
                    .inspectionCount(v.getQcInspectionCount())
                    .passedCount(v.getQcPassedCount())
                    .defectiveCount(v.getQcDefectiveCount())
                    .boxCount(v.getQcBoxCount())
                    .boxLengthCm(v.getQcBoxLengthCm())
                    .boxWidthCm(v.getQcBoxWidthCm())
                    .boxHeightCm(v.getQcBoxHeightCm())
                    .netWeightPerUnit(v.getQcNetWeightPerUnit())
                    .grossWeight(v.getQcGrossWeight())
                    .qcDate(v.getQcDate())
                    .status(v.getQcStatus())
                    .build();
        }

        LogisticsPlanVO logisticsPlanVO = null;
        if (v.getLogisticsPlanId() != null) {
            logisticsPlanVO = LogisticsPlanVO.builder()
                    .id(v.getLogisticsPlanId())
                    .planCode(v.getLogisticsPlanCode())
                    .procurementId(v.getProcurementId())
                    .factoryId(v.getProcurementFactoryId())
                    .productCode(v.getProcurementProductCode())
                    .subProductCode(v.getProcurementSubProductCode())
                    .planType(v.getLogisticsPlanType())
                    .status(v.getLogisticsStatus())
                    .cargoLengthCm(v.getLogisticsCargoLengthCm())
                    .cargoWidthCm(v.getLogisticsCargoWidthCm())
                    .cargoHeightCm(v.getLogisticsCargoHeightCm())
                    .cargoVolumeCbm(v.getLogisticsCargoVolumeCbm())
                    .cargoWeightKg(v.getLogisticsCargoWeightKg())
                    .quantity(v.getProcurementQuantity())
                    .requiresQc(v.getLogisticsRequiresQc())
                    .estimatedShipDate(v.getLogisticsEstimatedShipDate())
                    .actualShipDate(v.getLogisticsActualShipDate())
                    .build();
        }

        return OrderChainDetailVO.builder()
                .demandId(v.getDemandId())
                .demand(demandVO)
                .procurement(procurementVO)
                .factory(factoryVO)
                .qcRecord(qcRecordVO)
                .logisticsPlan(logisticsPlanVO)
                .domesticCustoms(null)
                .japanCustoms(null)
                .taxRefund(null)
                .salesRecord(null)
                .build();
    }
}
