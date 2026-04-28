package com.manpou.allinone.order.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.order.application.dto.OrderChainDetailVO;
import com.manpou.allinone.order.application.dto.OrderChainVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.FactoryVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.ReplenishmentDemandVO;
import com.manpou.allinone.order.domain.model.OrderChainView;
import com.manpou.allinone.order.domain.repository.OrderChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单总览 UseCase（Phase1 步骤1~4）。
 * 基于 v_order_chain_v1 视图提供列表和详情查询。
 */
@Service
@RequiredArgsConstructor
public class OrderChainUseCase {

    private final OrderChainRepository orderChainRepository;

    @Transactional(readOnly = true)
    public Page<OrderChainVO> getChainList(String demandStatus, String keyword, Pageable pageable) {
        return orderChainRepository.findChainList(demandStatus, keyword, pageable)
                .map(this::toChainVO);
    }

    @Transactional(readOnly = true)
    public OrderChainDetailVO getChainDetail(Long demandId) {
        OrderChainView view = orderChainRepository.findByDemandId(demandId)
                .orElseThrow(() -> BusinessException.notFound("Demand", demandId));
        return toDetailVO(view);
    }

    private OrderChainVO toChainVO(OrderChainView view) {
        return OrderChainVO.builder()
                .demandId(view.getDemandId())
                .demandCode(view.getDemandCode())
                .demandType(view.getDemandType())
                .demandProductCode(view.getDemandProductCode())
                .demandSubProductCode(view.getDemandSubProductCode())
                .demandQuantity(view.getDemandQuantity())
                .demandDestination(view.getDemandDestination())
                .demandJapanLead(view.getDemandJapanLead())
                .demandStatus(view.getDemandStatus())
                .linkedProcurementId(view.getLinkedProcurementId())
                .demandImageUrl(view.getDemandImageUrl())
                .demandCreateTime(view.getDemandCreateTime())
                .demandUpdateTime(view.getDemandUpdateTime())
                .procurementId(view.getProcurementId())
                .procurementCode(view.getProcurementCode())
                .procurementFactoryId(view.getProcurementFactoryId())
                .procurementProductCode(view.getProcurementProductCode())
                .procurementSubProductCode(view.getProcurementSubProductCode())
                .procurementQuantity(view.getProcurementQuantity())
                .procurementPriceRmb(view.getProcurementPriceRmb())
                .procurementTaxPoint(view.getProcurementTaxPoint())
                .procurementExchangeRate(view.getProcurementExchangeRate())
                .procurementBillingType(view.getProcurementBillingType())
                .procurementEstimatedPriceJpy(view.getProcurementEstimatedPriceJpy())
                .procurementOrderDate(view.getProcurementOrderDate())
                .procurementFactoryShipDate(view.getProcurementFactoryShipDate())
                .procurementPlannedShipDate(view.getProcurementPlannedShipDate())
                .procurementActualShipDate(view.getProcurementActualShipDate())
                .procurementLeadTimeDays(view.getProcurementLeadTimeDays())
                .procurementProductLead(view.getProcurementProductLead())
                .procurementJapanLead(view.getProcurementJapanLead())
                .procurementChinaLead(view.getProcurementChinaLead())
                .procurementDestination(view.getProcurementDestination())
                .procurementCustomerCompany(view.getProcurementCustomerCompany())
                .procurementStatus(view.getProcurementStatus())
                .procurementCreateTime(view.getProcurementCreateTime())
                .factoryId(view.getFactoryId())
                .factoryCode(view.getFactoryCode())
                .factoryName(view.getFactoryName())
                .factoryProvince(view.getFactoryProvince())
                .factoryCity(view.getFactoryCity())
                .factoryCounty(view.getFactoryCounty())
                .factoryContactName(view.getFactoryContactName())
                .factoryContactPhone(view.getFactoryContactPhone())
                .qcRecordId(view.getQcRecordId())
                .qcCode(view.getQcCode())
                .qcResult(view.getQcResult())
                .qcInspectionCount(view.getQcInspectionCount())
                .qcPassedCount(view.getQcPassedCount())
                .qcDefectiveCount(view.getQcDefectiveCount())
                .qcBoxCount(view.getQcBoxCount())
                .qcBoxLengthCm(view.getQcBoxLengthCm())
                .qcBoxWidthCm(view.getQcBoxWidthCm())
                .qcBoxHeightCm(view.getQcBoxHeightCm())
                .qcNetWeightPerUnit(view.getQcNetWeightPerUnit())
                .qcGrossWeight(view.getQcGrossWeight())
                .qcDate(view.getQcDate())
                .qcStatus(view.getQcStatus())
                .qcCreateTime(view.getQcCreateTime())
                .logisticsPlanId(view.getLogisticsPlanId())
                .logisticsPlanCode(view.getLogisticsPlanCode())
                .logisticsContainerNo(view.getLogisticsContainerNo())
                .logisticsPlanType(view.getLogisticsPlanType())
                .logisticsCargoLengthCm(view.getLogisticsCargoLengthCm())
                .logisticsCargoWidthCm(view.getLogisticsCargoWidthCm())
                .logisticsCargoHeightCm(view.getLogisticsCargoHeightCm())
                .logisticsCargoVolumeCbm(view.getLogisticsCargoVolumeCbm())
                .logisticsCargoWeightKg(view.getLogisticsCargoWeightKg())
                .logisticsRequiresQc(view.getLogisticsRequiresQc())
                .logisticsEstimatedShipDate(view.getLogisticsEstimatedShipDate())
                .logisticsActualShipDate(view.getLogisticsActualShipDate())
                .logisticsStatus(view.getLogisticsStatus())
                .logisticsCreateTime(view.getLogisticsCreateTime())
                .productNameZh(view.getProductNameZh())
                .productNameJa(view.getProductNameJa())
                .productCategory(view.getProductCategory())
                .step1Status(view.getStep1Status())
                .step2Status(view.getStep2Status())
                .step3Status(view.getStep3Status())
                .step4Status(view.getStep4Status())
                .build();
    }

    private OrderChainDetailVO toDetailVO(OrderChainView view) {
        return OrderChainDetailVO.builder()
                .demandId(view.getDemandId())
                .demand(toReplenishmentDemandVO(view))
                .procurement(toProcurementVO(view))
                .factory(toFactoryVO(view))
                .qcRecord(toQcRecordVO(view))
                .logisticsPlan(toLogisticsPlanVO(view))
                // Phase1 步骤5~8 占位 null
                .domesticCustoms(null)
                .japanCustoms(null)
                .taxRefund(null)
                .salesRecord(null)
                .build();
    }

    private ReplenishmentDemandVO toReplenishmentDemandVO(OrderChainView v) {
        return ReplenishmentDemandVO.builder()
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
    }

    private ProcurementVO toProcurementVO(OrderChainView v) {
        if (v.getProcurementId() == null) return null;
        return ProcurementVO.builder()
                .id(v.getProcurementId())
                .procurementCode(v.getProcurementCode())
                .factoryId(v.getProcurementFactoryId())
                .factoryName(v.getFactoryName())
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
    }

    private FactoryVO toFactoryVO(OrderChainView v) {
        if (v.getFactoryId() == null) return null;
        return FactoryVO.builder()
                .id(v.getFactoryId())
                .factoryCode(v.getFactoryCode())
                .factoryName(v.getFactoryName())
                .province(v.getFactoryProvince())
                .city(v.getFactoryCity())
                .county(v.getFactoryCounty())
                .contactName(v.getFactoryContactName())
                .contactPhone(v.getFactoryContactPhone())
                .build();
    }

    private QcRecordVO toQcRecordVO(OrderChainView v) {
        if (v.getQcRecordId() == null) return null;
        return QcRecordVO.builder()
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

    private LogisticsPlanVO toLogisticsPlanVO(OrderChainView v) {
        if (v.getLogisticsPlanId() == null) return null;
        return LogisticsPlanVO.builder()
                .id(v.getLogisticsPlanId())
                .planCode(v.getLogisticsPlanCode())
                .procurementId(v.getProcurementId())
                .factoryId(v.getFactoryId())
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
}
