package com.manpou.allinone.factory.application.assembler;

import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.application.dto.FactoryUpdateCmd;
import com.manpou.allinone.factory.domain.model.Factory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FactoryAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateFactoryCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("F-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public FactoryPageQuery toDto(Factory entity) {
        return FactoryPageQuery.builder()
                .id(entity.getId())
                .factoryCode(entity.getFactoryCode())
                .factoryName(entity.getFactoryName())
                .location(entity.getLocation())
                .roughLocation(entity.getRoughLocation())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Factory toEntity(FactoryCreateCmd cmd) {
        Factory entity = new Factory();
        entity.setFactoryCode(generateFactoryCode());
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(FactoryCreateCmd cmd, Factory entity) {
        entity.setFactoryName(cmd.getFactoryName());
        if (cmd.getLocation() != null) entity.setLocation(cmd.getLocation());
        if (cmd.getRoughLocation() != null) entity.setRoughLocation(cmd.getRoughLocation());
        if (cmd.getContactName() != null) entity.setContactName(cmd.getContactName());
        if (cmd.getContactPhone() != null) entity.setContactPhone(cmd.getContactPhone());
    }

    public void copyToEntity(FactoryUpdateCmd cmd, Factory entity) {
        if (cmd.getFactoryName() != null) entity.setFactoryName(cmd.getFactoryName());
        if (cmd.getLocation() != null) entity.setLocation(cmd.getLocation());
        if (cmd.getRoughLocation() != null) entity.setRoughLocation(cmd.getRoughLocation());
        if (cmd.getContactName() != null) entity.setContactName(cmd.getContactName());
        if (cmd.getContactPhone() != null) entity.setContactPhone(cmd.getContactPhone());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
    }
}
