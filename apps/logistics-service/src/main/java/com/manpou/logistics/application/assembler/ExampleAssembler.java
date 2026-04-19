package com.manpou.logistics.application.assembler;

import com.manpou.logistics.application.dto.ExampleCreateCmd;
import com.manpou.logistics.application.dto.ExamplePageQuery;
import com.manpou.logistics.application.dto.ExampleUpdateCmd;
import com.manpou.logistics.domain.model.Example;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class ExampleAssembler {

    public ExamplePageQuery toDto(Example entity) {
        return ExamplePageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Example toEntity(ExampleCreateCmd cmd) {
        Example entity = new Example();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(ExampleUpdateCmd cmd, Example entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
