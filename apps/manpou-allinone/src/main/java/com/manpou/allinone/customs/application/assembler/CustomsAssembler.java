package com.manpou.allinone.customs.application.assembler;

import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.domain.model.CustomsExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class CustomsAssembler {

    public CustomsPageQuery toDto(CustomsExample entity) {
        return CustomsPageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public CustomsExample toEntity(CustomsCreateCmd cmd) {
        CustomsExample entity = new CustomsExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(CustomsUpdateCmd cmd, CustomsExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
