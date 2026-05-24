package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.common.port.FactoryQueryPort;
import com.manpou.allinone.order.application.dto.OrderProcurementSelectorDTO;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ShipmentBatchRepository;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §API设计 完全对齐。
 * EV-107/SPEC-B11：移除了跨模块 QcRecordRepository 依赖，改为同模块 ShipmentBatchRepository。
 */
@Component
public class ProcurementAssembler {

    private final FactoryQueryPort factoryQueryPort;
    private final ShipmentBatchRepository shipmentBatchRepository;

    public ProcurementAssembler(FactoryQueryPort factoryQueryPort,
                               @org.springframework.beans.factory.annotation.Qualifier("jpaShipmentBatchRepository")
                               ShipmentBatchRepository shipmentBatchRepository) {
        this.factoryQueryPort = factoryQueryPort;
        this.shipmentBatchRepository = shipmentBatchRepository;
    }

    public ProcurementPageQuery toDto(Procurement entity) {
        String factoryName = null;
        if (entity.getFactoryId() != null) {
            factoryName = factoryQueryPort
                    .findByIdAndDeletedIsFalse(entity.getFactoryId())
                    .map(f -> f.getFactoryName())
                    .orElse(null);
        }
        // 批次数量 + 出货数量（Phase2：batchCount>0 → 已出货）
        long batchCount = shipmentBatchRepository.countByProcurementIdAndDeletedIsFalse(entity.getId());
        int shipmentQuantity = shipmentBatchRepository
                .findByProcurementIdAndDeletedIsFalse(entity.getId())
                .stream()
                .mapToInt(com.manpou.allinone.procurement.domain.model.ShipmentBatch::getShipmentQuantity)
                .sum();
        // legacy fallback: 无出货批次时使用 shipped_quantity（历史数据导入）
        if (shipmentQuantity == 0 && entity.getShippedQuantity() != null) {
            shipmentQuantity = entity.getShippedQuantity();
        }
        return ProcurementPageQuery.builder()
                .id(entity.getId())
                .factoryId(entity.getFactoryId())
                .factoryName(factoryName)
                .batchCount(Long.valueOf(batchCount))
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .shiban(entity.getShiban())
                .material(entity.getMaterial())
                .requiresQc(entity.getRequiresQc())
                .quantity(entity.getQuantity())
                .shipmentQuantity(shipmentQuantity)
                .priceRmb(entity.getPriceRmb())
                .exchangeRate(entity.getExchangeRate())
                .taxPoint(entity.getTaxPoint())
                .billingType(entity.getBillingType())
                .estimatedPriceJpy(entity.getEstimatedPriceJpy())
                .customsRemarks(entity.getCustomsRemarks())
                .instructionManual(entity.getInstructionManual())
                .orderDate(entity.getOrderDate())
                .factoryShipDate(entity.getFactoryShipDate())
                .plannedShipDate(entity.getPlannedShipDate())
                .actualShipDate(entity.getActualShipDate())
                .leadTimeDays(entity.getLeadTimeDays())
                .cartonNotes(entity.getCartonNotes())
                .remark(entity.getRemark())
                .group(entity.getGroup_())
                .afterSalesDeadline(entity.getAfterSalesDeadline())
                .productLead(entity.getProductLead())
                .japanLead(entity.getJapanLead())
                .chinaLead(entity.getChinaLead())
                .destination(entity.getDestination())
                .customerCompany(entity.getCustomerCompany())
                .status(entity.getStatus())
                .returnReason(entity.getReturnReason())
                .returnDate(entity.getReturnDate())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateBy(entity.getUpdateBy())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public OrderProcurementSelectorDTO toOrderProcurementSelectorDto(Procurement entity) {
        String factoryName = null;
        if (entity.getFactoryId() != null) {
            factoryName = factoryQueryPort
                    .findByIdAndDeletedIsFalse(entity.getFactoryId())
                    .map(f -> f.getFactoryName())
                    .orElse(null);
        }
        return OrderProcurementSelectorDTO.builder()
                .id(entity.getId())
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
                .destination(entity.getDestination())
                .customerCompany(entity.getCustomerCompany())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .orderDate(entity.getOrderDate())
                .plannedShipDate(entity.getPlannedShipDate())
                .createTime(entity.getCreateTime())
                .build();
    }

    public Procurement toEntity(ProcurementCreateCmd cmd) {
        Procurement entity = new Procurement();
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(ProcurementCreateCmd cmd, Procurement entity) {
        if (cmd.getFactoryId() != null) entity.setFactoryId(cmd.getFactoryId());
        entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getShiban() != null) entity.setShiban(cmd.getShiban());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        entity.setQuantity(cmd.getQuantity());
        entity.setPriceRmb(cmd.getPriceRmb());
        entity.setExchangeRate(cmd.getExchangeRate());
        entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getBillingType() != null) entity.setBillingType(cmd.getBillingType());
        if (cmd.getCustomsRemarks() != null) entity.setCustomsRemarks(cmd.getCustomsRemarks());
        if (cmd.getInstructionManual() != null) entity.setInstructionManual(cmd.getInstructionManual());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getPlannedShipDate() != null) entity.setPlannedShipDate(cmd.getPlannedShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getLeadTimeDays() != null) entity.setLeadTimeDays(cmd.getLeadTimeDays());
        if (cmd.getCartonNotes() != null) entity.setCartonNotes(cmd.getCartonNotes());
        if (cmd.getRemark() != null) entity.setRemark(cmd.getRemark());
        if (cmd.getGroup() != null) entity.setGroup_(cmd.getGroup());
        if (cmd.getAfterSalesDeadline() != null) entity.setAfterSalesDeadline(cmd.getAfterSalesDeadline());
        if (cmd.getProductLead() != null) entity.setProductLead(cmd.getProductLead());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getChinaLead() != null) entity.setChinaLead(cmd.getChinaLead());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getCustomerCompany() != null) entity.setCustomerCompany(cmd.getCustomerCompany());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
    }

    public void copyToEntity(ProcurementUpdateCmd cmd, Procurement entity) {
        // factoryId 不允许在更新时修改，由 UseCase 层校验
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getShiban() != null) entity.setShiban(cmd.getShiban());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getPriceRmb() != null) entity.setPriceRmb(cmd.getPriceRmb());
        if (cmd.getExchangeRate() != null) entity.setExchangeRate(cmd.getExchangeRate());
        if (cmd.getTaxPoint() != null) entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getBillingType() != null) entity.setBillingType(cmd.getBillingType());
        if (cmd.getCustomsRemarks() != null) entity.setCustomsRemarks(cmd.getCustomsRemarks());
        if (cmd.getInstructionManual() != null) entity.setInstructionManual(cmd.getInstructionManual());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getPlannedShipDate() != null) entity.setPlannedShipDate(cmd.getPlannedShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getLeadTimeDays() != null) entity.setLeadTimeDays(cmd.getLeadTimeDays());
        if (cmd.getCartonNotes() != null) entity.setCartonNotes(cmd.getCartonNotes());
        if (cmd.getRemark() != null) entity.setRemark(cmd.getRemark());
        if (cmd.getGroup() != null) entity.setGroup_(cmd.getGroup());
        if (cmd.getAfterSalesDeadline() != null) entity.setAfterSalesDeadline(cmd.getAfterSalesDeadline());
        if (cmd.getProductLead() != null) entity.setProductLead(cmd.getProductLead());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getChinaLead() != null) entity.setChinaLead(cmd.getChinaLead());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getCustomerCompany() != null) entity.setCustomerCompany(cmd.getCustomerCompany());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus(), cmd.getReturnReason());
    }
}
