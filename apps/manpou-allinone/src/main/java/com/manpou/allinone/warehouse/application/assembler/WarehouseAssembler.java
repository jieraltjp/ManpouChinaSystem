package com.manpou.allinone.warehouse.application.assembler;

import com.manpou.allinone.warehouse.application.dto.WarehouseCreateCmd;
import com.manpou.allinone.warehouse.application.dto.WarehousePageQuery;
import com.manpou.allinone.warehouse.application.dto.WarehouseUpdateCmd;
import com.manpou.allinone.warehouse.domain.model.WarehouseExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class WarehouseAssembler {

    public WarehousePageQuery toDto(WarehouseExample entity) {
        return WarehousePageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public WarehouseExample toEntity(WarehouseCreateCmd cmd) {
        WarehouseExample entity = new WarehouseExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(WarehouseUpdateCmd cmd, WarehouseExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
