package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.procurement.application.dto.ShipmentBatchCreateCmd;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchPageQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchUpdateCmd;
import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import org.springframework.stereotype.Component;

/**
 * 出货批次 DTO ↔ Entity 转换器（SPEC-B11）。
 */
@Component
public class ShipmentBatchAssembler {

    public ShipmentBatchPageQuery toDto(ShipmentBatch entity) {
        return ShipmentBatchPageQuery.builder()
                .id(entity.getId())
                .procurementId(entity.getProcurementId())
                .batchCode(entity.getBatchCode())
                .shipmentQuantity(entity.getShipmentQuantity())
                .factoryShipDate(entity.getFactoryShipDate())
                .actualShipDate(entity.getActualShipDate())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateBy(entity.getUpdateBy())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public ShipmentBatch toEntity(ShipmentBatchCreateCmd cmd) {
        ShipmentBatch entity = new ShipmentBatch();
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(ShipmentBatchCreateCmd cmd, ShipmentBatch entity) {
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getBatchCode() != null) entity.setBatchCode(cmd.getBatchCode());
        if (cmd.getShipmentQuantity() != null) entity.setShipmentQuantity(cmd.getShipmentQuantity());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyToEntity(ShipmentBatchUpdateCmd cmd, ShipmentBatch entity) {
        if (cmd.getBatchCode() != null) entity.setBatchCode(cmd.getBatchCode());
        if (cmd.getShipmentQuantity() != null) entity.setShipmentQuantity(cmd.getShipmentQuantity());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }
}
