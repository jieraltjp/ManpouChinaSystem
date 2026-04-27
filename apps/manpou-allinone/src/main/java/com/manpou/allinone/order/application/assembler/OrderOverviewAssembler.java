package com.manpou.allinone.order.application.assembler;

import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.DemandVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.DomesticCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.FactoryVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.JapanCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.SalesRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.StepStatus;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.TaxRefundVO;
import com.manpou.allinone.procurement.domain.model.BillingType;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import org.springframework.stereotype.Component;

/**
 * 订单总览 DTO 转换器。
 * 只做类型映射，不含业务判断。
 */
@Component
public class OrderOverviewAssembler {

    public ProcurementVO toProcurementVO(Procurement entity, String factoryName) {
        return ProcurementVO.builder()
                .id(entity.getId())
                .procurementCode("P-" + entity.getId())
                .factoryId(entity.getFactoryId())
                .factoryName(factoryName)
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .material(entity.getMaterial())
                .requiresQc(entity.getRequiresQc())
                .quantity(entity.getQuantity())
                .priceRmb(entity.getPriceRmb())
                .exchangeRate(entity.getExchangeRate())
                .taxPoint(entity.getTaxPoint())
                .billingType(entity.getBillingType() != null ? entity.getBillingType().name() : null)
                .estimatedPriceJpy(entity.getEstimatedPriceJpy())
                .orderDate(entity.getOrderDate())
                .factoryShipDate(entity.getFactoryShipDate())
                .plannedShipDate(entity.getPlannedShipDate())
                .actualShipDate(entity.getActualShipDate())
                .productLead(entity.getProductLead())
                .japanLead(entity.getJapanLead())
                .chinaLead(entity.getChinaLead())
                .destination(entity.getDestination())
                .customerCompany(entity.getCustomerCompany())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createTime(entity.getCreateTime())
                .build();
    }

    public FactoryVO toFactoryVO(com.manpou.allinone.factory.domain.model.Factory entity) {
        return FactoryVO.builder()
                .id(entity.getId())
                .factoryCode(entity.getFactoryCode())
                .factoryName(entity.getFactoryName())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .province(entity.getProvince())
                .city(entity.getCity())
                .county(entity.getCounty())
                .roughLocation(entity.getRoughLocation())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .cooperationStatus(entity.getCooperationStatus() != null ? entity.getCooperationStatus().name() : null)
                .build();
    }

    public DemandVO toDemandVO(ReplenishmentDemand entity) {
        return DemandVO.builder()
                .id(entity.getId())
                .demandCode(entity.getDemandCode())
                .demandType(entity.getDemandType() != null ? entity.getDemandType().name() : null)
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .quantity(entity.getQuantity())
                .destination(entity.getDestination())
                .japanLead(entity.getJapanLead())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createTime(entity.getCreateTime())
                .build();
    }

    public QcRecordVO toQcVO(QcRecord entity) {
        return QcRecordVO.builder()
                .id(entity.getId())
                .qcCode(entity.getQcCode())
                .procurementId(entity.getProcurementId())
                .sellerName(entity.getSellerName())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .result(entity.getResult() != null ? entity.getResult().name() : null)
                .inspectionCount(entity.getInspectionCount())
                .passedCount(entity.getPassedCount())
                .defectiveCount(entity.getDefectiveCount())
                .boxCount(entity.getBoxCount())
                .boxLengthCm(entity.getBoxLengthCm())
                .boxWidthCm(entity.getBoxWidthCm())
                .boxHeightCm(entity.getBoxHeightCm())
                .netWeightPerUnit(entity.getNetWeightPerUnit())
                .grossWeight(entity.getGrossWeight())
                .qcDate(entity.getQcDate())
                .qcUserId(entity.getQcUserId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .build();
    }

    public LogisticsPlanVO toLogisticsVO(LogisticsPlan entity) {
        return LogisticsPlanVO.builder()
                .id(entity.getId())
                .planCode(entity.getPlanCode())
                .procurementId(entity.getProcurementId())
                .factoryId(entity.getFactoryId())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .planType(entity.getPlanType() != null ? entity.getPlanType().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .cargoLengthCm(entity.getCargoLengthCm())
                .cargoWidthCm(entity.getCargoWidthCm())
                .cargoHeightCm(entity.getCargoHeightCm())
                .cargoVolumeCbm(entity.getCargoVolumeCbm())
                .cargoWeightKg(entity.getCargoWeightKg())
                .quantity(entity.getQuantity())
                .requiresQc(entity.getRequiresQc())
                .estimatedShipDate(entity.getEstimatedShipDate())
                .actualShipDate(entity.getActualShipDate())
                .build();
    }

    public DomesticCustomsVO toDomesticCustomsVO(DomesticCustomsRecord entity) {
        return DomesticCustomsVO.builder()
                .id(entity.getId())
                .customsCode(entity.getCustomsCode())
                .procurementId(entity.getProcurementId())
                .productCode(entity.getProductCode())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .estimatedValueCny(entity.getEstimatedValueCny())
                .remarks(entity.getRemarks())
                .build();
    }

    public JapanCustomsVO toJapanCustomsVO(JapanCustomsRecord entity) {
        return JapanCustomsVO.builder()
                .id(entity.getId())
                .procurementId(entity.getProcurementId())
                .domesticCustomsId(entity.getDomesticCustomsId())
                .logisticsPlanId(entity.getLogisticsPlanId())
                .customsEntryNo(entity.getCustomsEntryNo())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .arrivalDate(entity.getArrivalDate())
                .clearanceDate(entity.getClearanceDate())
                .customsBroker(entity.getCustomsBroker())
                .brokerPhone(entity.getBrokerPhone())
                .brokerContact(entity.getBrokerContact())
                .importDutyPaid(entity.getImportDutyPaid())
                .consumptionTaxPaid(entity.getConsumptionTaxPaid())
                .arrivalPort(entity.getArrivalPort())
                .declaredWeightKg(entity.getDeclaredWeightKg())
                .declaredVolumeCbm(entity.getDeclaredVolumeCbm())
                .remarks(entity.getRemarks())
                .build();
    }

    public TaxRefundVO toTaxRefundVO(TaxRefundRecord entity) {
        return TaxRefundVO.builder()
                .id(entity.getId())
                .procurementId(entity.getProcurementId())
                .japanCustomsId(entity.getJapanCustomsId())
                .refundCode(entity.getRefundCode())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .billingType(entity.getBillingType() != null ? entity.getBillingType().name() : null)
                .priceRmb(entity.getPriceRmb())
                .taxPoint(entity.getTaxPoint())
                .quantity(entity.getQuantity())
                .estimatedRefundRmb(entity.getEstimatedRefundRmb())
                .actualRefundRmb(entity.getActualRefundRmb())
                .exchangeRate(entity.getExchangeRate())
                .refundDate(entity.getRefundDate())
                .refundBank(entity.getRefundBank())
                .remarks(entity.getRemarks())
                .build();
    }

    public SalesRecordVO toSalesRecordVO(SalesRecord entity) {
        return SalesRecordVO.builder()
                .id(entity.getId())
                .recordCode(entity.getRecordCode())
                .procurementId(entity.getProcurementId())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .salesChannel(entity.getSalesChannel() != null ? entity.getSalesChannel().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .listingDate(entity.getListingDate())
                .initialStock(entity.getInitialStock())
                .currentStock(entity.getCurrentStock())
                .safetyStock(entity.getSafetyStock())
                .salesQuantity(entity.getSalesQuantity())
                .returnedQuantity(entity.getReturnedQuantity())
                .returnRate(entity.getReturnRate())
                .sellingPriceJpy(entity.getSellingPriceJpy())
                .remarks(entity.getRemarks())
                .build();
    }

    /**
     * Demand 锚点专用状态数组。
     * 规则：step1=COMPLETED（需求单存在），step2-8=NOT_STARTED
     */
    public StepStatus[] computeDemandStepStatuses() {
        StepStatus[] result = new StepStatus[8];
        result[0] = StepStatus.COMPLETED;
        for (int i = 1; i < 8; i++) {
            result[i] = StepStatus.NOT_STARTED;
        }
        return result;
    }

    /**
     * 计算 8 步状态数组。
     * 规则：
     * - NOT_STARTED: 无对应记录
     * - IN_PROGRESS: 有记录且非终态
     * - COMPLETED: 有记录且为终态
     */
    public StepStatus[] computeStepStatuses(
            Procurement procurement,
            DemandVO demand,
            QcRecordVO qc,
            LogisticsPlanVO lp,
            DomesticCustomsVO dc,
            JapanCustomsVO jp,
            TaxRefundVO tr,
            SalesRecordVO sr) {

        StepStatus[] result = new StepStatus[8];

        // 步骤1 补货：demand != null → COMPLETED
        result[0] = demand != null ? StepStatus.COMPLETED : StepStatus.NOT_STARTED;

        // 步骤2 发注：procurement != null → COMPLETED（锚点必有）
        result[1] = StepStatus.COMPLETED;

        // 步骤3 验货：终态 COMPLETED / RETURN_REQUESTED
        if (qc == null) {
            result[2] = StepStatus.NOT_STARTED;
        } else if ("COMPLETED".equals(qc.getStatus()) || "RETURN_REQUESTED".equals(qc.getStatus())) {
            result[2] = StepStatus.COMPLETED;
        } else {
            result[2] = StepStatus.IN_PROGRESS;
        }

        // 步骤4 调配：终态 DELIVERED
        if (lp == null) {
            result[3] = StepStatus.NOT_STARTED;
        } else if (LogisticsStatus.DELIVERED.name().equals(lp.getStatus())) {
            result[3] = StepStatus.COMPLETED;
        } else {
            result[3] = StepStatus.IN_PROGRESS;
        }

        // 步骤5 国内报关：终态 CLEARED
        if (dc == null) {
            result[4] = StepStatus.NOT_STARTED;
        } else if ("CLEARED".equals(dc.getStatus())) {
            result[4] = StepStatus.COMPLETED;
        } else {
            result[4] = StepStatus.IN_PROGRESS;
        }

        // 步骤6 日本清关：终态 CLEARED
        if (jp == null) {
            result[5] = StepStatus.NOT_STARTED;
        } else if ("CLEARED".equals(jp.getStatus())) {
            result[5] = StepStatus.COMPLETED;
        } else {
            result[5] = StepStatus.IN_PROGRESS;
        }

        // 步骤7 退税：终态 COMPLETED / NO_REFUND
        if (tr == null) {
            result[6] = StepStatus.NOT_STARTED;
        } else if ("COMPLETED".equals(tr.getStatus()) || "NO_REFUND".equals(tr.getStatus())) {
            result[6] = StepStatus.COMPLETED;
        } else {
            result[6] = StepStatus.IN_PROGRESS;
        }

        // 步骤8 运营：终态 DISCONTINUED
        if (sr == null) {
            result[7] = StepStatus.NOT_STARTED;
        } else if ("DISCONTINUED".equals(sr.getStatus())) {
            result[7] = StepStatus.COMPLETED;
        } else {
            result[7] = StepStatus.IN_PROGRESS;
        }

        return result;
    }
}
