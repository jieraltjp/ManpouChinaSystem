package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.Procurement;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 * 与 docs/business/SPEC-发注管理流程.md §3.2 完全对齐。
 */
@Component
public class ProcurementAssembler {

    public ProcurementPageQuery toDto(Procurement entity) {
        return ProcurementPageQuery.builder()
                .id(entity.getId())
                .factoryId(entity.getFactoryId())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .material(entity.getMaterial())
                .requiresQc(entity.getRequiresQc())
                .quantity(entity.getQuantity())
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
                .productLead(entity.getProductLead())
                .japanLead(entity.getJapanLead())
                .chinaLead(entity.getChinaLead())
                .destination(entity.getDestination())
                .customerCompany(entity.getCustomerCompany())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Procurement toEntity(ProcurementCreateCmd cmd) {
        Procurement entity = new Procurement();
        copyToEntity(cmd, entity);
        entity.calculateEstimatedPriceJpy();
        return entity;
    }

    public void copyToEntity(ProcurementCreateCmd cmd, Procurement entity) {
        if (cmd.getFactoryId() != null) entity.setFactoryId(cmd.getFactoryId());
        entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
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
        if (cmd.getProductLead() != null) entity.setProductLead(cmd.getProductLead());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getChinaLead() != null) entity.setChinaLead(cmd.getChinaLead());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getCustomerCompany() != null) entity.setCustomerCompany(cmd.getCustomerCompany());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
    }

    public void copyToEntity(ProcurementUpdateCmd cmd, Procurement entity) {
        if (cmd.getFactoryId() != null) entity.setFactoryId(cmd.getFactoryId());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
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
        if (cmd.getProductLead() != null) entity.setProductLead(cmd.getProductLead());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getChinaLead() != null) entity.setChinaLead(cmd.getChinaLead());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getCustomerCompany() != null) entity.setCustomerCompany(cmd.getCustomerCompany());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
        // 价格变动时重新计算估算价
        if (cmd.getPriceRmb() != null || cmd.getExchangeRate() != null || cmd.getTaxPoint() != null) {
            entity.calculateEstimatedPriceJpy();
        }
    }
}
