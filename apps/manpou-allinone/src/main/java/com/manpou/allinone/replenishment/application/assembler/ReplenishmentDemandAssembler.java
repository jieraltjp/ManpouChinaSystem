package com.manpou.allinone.replenishment.application.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReplenishmentDemandAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                .subProductCodes(parseSubProductCodes(entity.getSubProductCode()))
                .quantity(entity.getQuantity())
                .destination(entity.getDestination())
                .japanLead(entity.getJapanLead())
                .status(entity.getStatus())
                .linkedProcurementId(entity.getLinkedProcurementId())
                .remarks(entity.getRemarks())
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
        entity.setSubProductCode(serializeSubProductCodes(cmd.getSubProductCodes()));
        entity.setQuantity(cmd.getQuantity());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyToEntity(ReplenishmentDemandUpdateCmd cmd, ReplenishmentDemand entity) {
        if (cmd.getDemandType() != null) entity.setDemandType(cmd.getDemandType());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCodes() != null) entity.setSubProductCode(serializeSubProductCodes(cmd.getSubProductCodes()));
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getStatus() != null) {
            if (cmd.getStatus() == com.manpou.allinone.replenishment.domain.model.DemandStatus.CANCELLED) {
                entity.cancel();
            }
        }
    }

    /** 序列化 List<String> → JSON 字符串存储 */
    private String serializeSubProductCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) return null;
        if (codes.size() == 1) return codes.get(0);
        try {
            return MAPPER.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            return codes.get(0);
        }
    }

    /** 反序列化 DB 字符串 → List<String>（兼容旧数据） */
    private List<String> parseSubProductCodes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        raw = raw.trim();
        if (raw.startsWith("[")) {
            try {
                return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                return List.of(raw);
            }
        }
        return List.of(raw);
    }
}
