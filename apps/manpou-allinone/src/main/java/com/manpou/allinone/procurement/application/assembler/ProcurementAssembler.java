package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.ProcurementExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class ProcurementAssembler {

    public ProcurementPageQuery toDto(ProcurementExample entity) {
        return ProcurementPageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public ProcurementExample toEntity(ProcurementCreateCmd cmd) {
        ProcurementExample entity = new ProcurementExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(ProcurementUpdateCmd cmd, ProcurementExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
