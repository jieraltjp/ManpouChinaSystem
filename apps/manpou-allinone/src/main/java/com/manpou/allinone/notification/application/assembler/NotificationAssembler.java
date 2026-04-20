package com.manpou.allinone.notification.application.assembler;

import com.manpou.allinone.notification.application.dto.NotificationCreateCmd;
import com.manpou.allinone.notification.application.dto.NotificationPageQuery;
import com.manpou.allinone.notification.application.dto.NotificationUpdateCmd;
import com.manpou.allinone.notification.domain.model.NotificationExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class NotificationAssembler {

    public NotificationPageQuery toDto(NotificationExample entity) {
        return NotificationPageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public NotificationExample toEntity(NotificationCreateCmd cmd) {
        NotificationExample entity = new NotificationExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(NotificationUpdateCmd cmd, NotificationExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
