package com.manpou.allinone.customs.application.assembler;

import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import org.springframework.stereotype.Component;

/**
 * 国内报关记录 DTO ↔ Entity 转换器。
 */
@Component
public class CustomsAssembler {

    public CustomsPageQuery toDto(DomesticCustomsRecord entity) {
        return CustomsPageQuery.builder()
                .id(entity.getId())
                .customsCode(entity.getCustomsCode())
                .customsDeclarationNo(entity.getCustomsDeclarationNo())
                .containerNo(entity.getContainerNo())
                .procurementId(entity.getProcurementId())
                .logisticsPlanId(entity.getLogisticsPlanId())
                .factoryId(entity.getFactoryId())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .quantity(entity.getQuantity())
                .estimatedValueCny(entity.getEstimatedValueCny())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public DomesticCustomsRecord toEntity(CustomsCreateCmd cmd) {
        DomesticCustomsRecord entity = new DomesticCustomsRecord();
        entity.setCustomsCode(generateCustomsCode());
        entity.setContainerNo(cmd.getContainerNo());
        entity.setProcurementId(cmd.getProcurementId());
        entity.setLogisticsPlanId(cmd.getLogisticsPlanId());
        entity.setFactoryId(cmd.getFactoryId());
        entity.setProductCode(cmd.getProductCode());
        entity.setSubProductCode(cmd.getSubProductCode());
        entity.setQuantity(cmd.getQuantity());
        entity.setEstimatedValueCny(cmd.getEstimatedValueCny());
        entity.setRemarks(cmd.getRemarks());
        return entity;
    }

    public void copyToEntity(CustomsUpdateCmd cmd, DomesticCustomsRecord entity) {
        if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getFactoryId() != null) entity.setFactoryId(cmd.getFactoryId());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getEstimatedValueCny() != null) entity.setEstimatedValueCny(cmd.getEstimatedValueCny());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getCustomsDeclarationNo() != null) entity.setCustomsDeclarationNo(cmd.getCustomsDeclarationNo());
    }

    private static String generateCustomsCode() {
        return "DC-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%03d", (int) (System.currentTimeMillis() % 1000));
    }
}
