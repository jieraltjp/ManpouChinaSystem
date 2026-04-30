package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.logistics.application.dto.ConsolidationPoolCreateCmd;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolPageQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolUpdateCmd;
import com.manpou.allinone.logistics.domain.model.ConsolidationPool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ConsolidationPoolAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generatePoolCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("CP-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public ConsolidationPoolPageQuery toDto(ConsolidationPool entity) {
        return ConsolidationPoolPageQuery.builder()
                .id(entity.getId())
                .poolCode(entity.getPoolCode())
                .destinationPort(entity.getDestinationPort())
                .totalCbm(entity.getTotalCbm())
                .totalWeightKg(entity.getTotalWeightKg())
                .planCount(entity.getPlanCount())
                .containerThresholdCbm(entity.getContainerThresholdCbm())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public ConsolidationPool toEntity(ConsolidationPoolCreateCmd cmd) {
        ConsolidationPool entity = new ConsolidationPool();
        entity.setPoolCode(generatePoolCode());
        entity.setDestinationPort(cmd.getDestinationPort());
        if (cmd.getContainerThresholdCbm() != null) {
            entity.setContainerThresholdCbm(cmd.getContainerThresholdCbm());
        } else {
            entity.setContainerThresholdCbm(new BigDecimal("70"));
        }
        return entity;
    }

    public void copyUpdate(ConsolidationPoolUpdateCmd cmd, ConsolidationPool entity) {
        if (cmd.getDestinationPort() != null) entity.setDestinationPort(cmd.getDestinationPort());
        if (cmd.getContainerThresholdCbm() != null) entity.setContainerThresholdCbm(cmd.getContainerThresholdCbm());
        if (cmd.getStatus() != null) entity.advanceStatus(cmd.getStatus());
    }
}
