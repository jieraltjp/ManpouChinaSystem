package com.manpou.allinone.replenishment.application.assembler;

import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingCreateCmd;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingPageQuery;
import com.manpou.allinone.replenishment.domain.model.DemandProcurementMapping;
import org.springframework.stereotype.Component;

/**
 * 需求-采购分配映射 DTO ↔ Entity 转换器（SPEC-B11）。
 */
@Component
public class DemandProcurementMappingAssembler {

    public DemandProcurementMappingPageQuery toDto(DemandProcurementMapping entity) {
        return DemandProcurementMappingPageQuery.builder()
                .id(entity.getId())
                .demandId(entity.getDemandId())
                .procurementId(entity.getProcurementId())
                .allocatedQuantity(entity.getAllocatedQuantity())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .build();
    }

    public DemandProcurementMapping toEntity(DemandProcurementMappingCreateCmd cmd) {
        DemandProcurementMapping entity = new DemandProcurementMapping();
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(DemandProcurementMappingCreateCmd cmd, DemandProcurementMapping entity) {
        if (cmd.getDemandId() != null) entity.setDemandId(cmd.getDemandId());
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getAllocatedQuantity() != null) entity.setAllocatedQuantity(cmd.getAllocatedQuantity());
    }
}
