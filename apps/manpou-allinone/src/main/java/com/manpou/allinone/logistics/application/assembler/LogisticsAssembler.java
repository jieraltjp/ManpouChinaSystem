package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.logistics.application.dto.LogisticsCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsUpdateCmd;
import com.manpou.allinone.logistics.domain.model.LogisticsExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class LogisticsAssembler {

    public LogisticsPageQuery toDto(LogisticsExample entity) {
        return LogisticsPageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public LogisticsExample toEntity(LogisticsCreateCmd cmd) {
        LogisticsExample entity = new LogisticsExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(LogisticsUpdateCmd cmd, LogisticsExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
