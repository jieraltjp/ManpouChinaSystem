package com.manpou.allinone.replenishment.application.assembler;

import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DTO ↔ Entity 转换器（v2.0.0）。
 * 直接字段映射，无需 JSON 序列化。
 */
@Component
public class ReplenishmentDemandAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateDemandCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("DM-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public ReplenishmentDemandPageQuery toDto(ReplenishmentDemand entity) {
        return ReplenishmentDemandPageQuery.builder()
                .id(entity.getId())
                .demandCode(entity.getDemandCode())
                .demandType(entity.getDemandType())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .quantity(entity.getQuantity())
                .destination(entity.getDestination())
                .japanLead(entity.getJapanLead())
                .status(entity.getStatus())
                .linkedProcurementId(entity.getLinkedProcurementId())
                .remarks(entity.getRemarks())
                .imageUrl(entity.getImageUrl())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public ReplenishmentDemand toEntity(ReplenishmentDemandCreateCmd cmd) {
        ReplenishmentDemand entity = new ReplenishmentDemand();
        entity.setDemandCode(generateDemandCode());
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(ReplenishmentDemandCreateCmd cmd, ReplenishmentDemand entity) {
        if (cmd.getDemandType() != null) entity.setDemandType(cmd.getDemandType());
        entity.setProductCode(cmd.getProductCode());
        entity.setSubProductCode(cmd.getSubProductCode());
        entity.setQuantity(cmd.getQuantity());
        entity.setDestination(cmd.getDestination());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyToEntity(ReplenishmentDemandUpdateCmd cmd, ReplenishmentDemand entity) {
        if (cmd.getDemandType() != null) entity.setDemandType(cmd.getDemandType());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
    }
}
