package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.Procurement;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class ProcurementAssembler {

    public ProcurementPageQuery toDto(Procurement entity) {
        return ProcurementPageQuery.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .quantity(entity.getQuantity())
                .priceRmb(entity.getPriceRmb())
                .exchangeRate(entity.getExchangeRate())
                .taxPoint(entity.getTaxPoint())
                .estimatedPriceJpy(entity.getEstimatedPriceJpy())
                .billingMethod(entity.getBillingMethod())
                .orderDate(entity.getOrderDate())
                .factoryShipDate(entity.getFactoryShipDate())
                .plannedShipDate(entity.getPlannedShipDate())
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
        // 计算估算批发价
        entity.calculateEstimatedPriceJpy();
        return entity;
    }

    public void copyToEntity(ProcurementCreateCmd cmd, Procurement entity) {
        entity.setProductCode(cmd.getProductCode());
        entity.setQuantity(cmd.getQuantity());
        entity.setPriceRmb(cmd.getPriceRmb());
        entity.setExchangeRate(cmd.getExchangeRate());
        entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getBillingMethod() != null) entity.setBillingMethod(cmd.getBillingMethod());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getPlannedShipDate() != null) entity.setPlannedShipDate(cmd.getPlannedShipDate());
        if (cmd.getProductLead() != null) entity.setProductLead(cmd.getProductLead());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getChinaLead() != null) entity.setChinaLead(cmd.getChinaLead());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getCustomerCompany() != null) entity.setCustomerCompany(cmd.getCustomerCompany());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
    }

    public void copyToEntity(ProcurementUpdateCmd cmd, Procurement entity) {
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getPriceRmb() != null) entity.setPriceRmb(cmd.getPriceRmb());
        if (cmd.getExchangeRate() != null) entity.setExchangeRate(cmd.getExchangeRate());
        if (cmd.getTaxPoint() != null) entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getBillingMethod() != null) entity.setBillingMethod(cmd.getBillingMethod());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getPlannedShipDate() != null) entity.setPlannedShipDate(cmd.getPlannedShipDate());
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
