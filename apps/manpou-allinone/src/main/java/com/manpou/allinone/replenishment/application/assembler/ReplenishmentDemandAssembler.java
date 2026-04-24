package com.manpou.allinone.replenishment.application.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.replenishment.application.dto.LinkedDemandItemDto;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.application.dto.SubProductItemDto;
import com.manpou.allinone.replenishment.domain.model.LinkedDemandItem;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DTO ↔ Entity 转换器（v1.6.0）。
 * 负责 subProductItems / linkedDemandItems JSON 序列化与反序列化。
 * 兼容旧数据（v1.5.x）：sub_product_code 存的是 List&lt;String&gt;，自动升格式。
 */
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
                .subProductItems(parseSubProductItems(entity.getSubProductItemsRaw()))
                .japanLead(entity.getJapanLead())
                .status(entity.getStatus())
                .linkedDemandItems(parseLinkedDemandItems(entity.getLinkedDemandItemsRaw()))
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
        entity.setSubProductItemsRaw(serializeSubProductItems(cmd.getSubProductItems()));
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyToEntity(ReplenishmentDemandUpdateCmd cmd, ReplenishmentDemand entity) {
        if (cmd.getDemandType() != null) entity.setDemandType(cmd.getDemandType());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductItems() != null) {
            entity.setSubProductItemsRaw(serializeSubProductItems(cmd.getSubProductItems()));
        }
        if (cmd.getJapanLead() != null) entity.setJapanLead(cmd.getJapanLead());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getStatus() != null) {
            if (cmd.getStatus() == com.manpou.allinone.replenishment.domain.model.DemandStatus.CANCELLED) {
                entity.cancel();
            }
        }
    }

    public void applyConvertedState(ReplenishmentDemand entity, List<LinkedDemandItem> linkedItems) {
        entity.markAsConverted();
        entity.setLinkedDemandItemsRaw(serializeLinkedDemandItems(linkedItems));
    }

    // ===== SubProductItem JSON =====

    /**
     * 序列化 SubProductItemDto 列表 → JSON 字符串。
     * 用于保存到 DB。
     */
    public String serializeSubProductItems(List<SubProductItemDto> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 反序列化 DB 字符串 → SubProductItem 列表（v1.6.0 兼容旧数据）。
     * - 新数据（v1.6.0）：[{"subCode":"be","quantity":100,"destination":"久留米"},...]
     * - 旧数据（v1.5.x）：["be","bu","re"] → 自动升级为 [{subCode:"be"},{subCode:"bu"},...]
     */
    public List<SubProductItem> parseSubProductItems(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        raw = raw.trim();
        if (raw.startsWith("[")) {
            try {
                var node = MAPPER.readTree(raw);
                if (node.isArray() && node.size() > 0) {
                    var first = node.get(0);
                    if (first.isTextual()) {
                        // 旧数据：List<String>，升级格式
                        List<String> codes = MAPPER.readValue(raw, new TypeReference<List<String>>() {});
                        return codes.stream().map(SubProductItem::new).toList();
                    } else {
                        // 新数据：List<SubProductItem>
                        return MAPPER.readValue(raw, new TypeReference<List<SubProductItem>>() {});
                    }
                }
            } catch (JsonProcessingException e) {
                // fall through
            }
        }
        // 单字符串（退化）
        return List.of(new SubProductItem(raw));
    }

    // ===== LinkedDemandItem JSON =====

    public String serializeLinkedDemandItems(List<LinkedDemandItem> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public List<LinkedDemandItem> parseLinkedDemandItems(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        raw = raw.trim();
        if (raw.startsWith("[")) {
            try {
                return MAPPER.readValue(raw, new TypeReference<List<LinkedDemandItem>>() {});
            } catch (JsonProcessingException e) {
                return List.of();
            }
        }
        return List.of();
    }
}
