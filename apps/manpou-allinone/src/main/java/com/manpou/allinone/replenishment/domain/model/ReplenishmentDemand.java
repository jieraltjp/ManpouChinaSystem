package com.manpou.allinone.replenishment.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


/**
 * 补货需求单实体（v1.6.0）。
 * 对应业务流第一步入口：非新品补货 or 新品采购。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md §2 聚合根 完全对齐。
 */
@Entity
@Table(name = "replenishment_demand", indexes = {
        @Index(name = "idx_demand_status", columnList = "status"),
        @Index(name = "idx_demand_type", columnList = "demand_type"),
        @Index(name = "idx_demand_product_code", columnList = "product_code")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class ReplenishmentDemand extends BaseEntity {

    @Column(name = "demand_code", unique = true, nullable = false, length = 32)
    private String demandCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type", nullable = false, length = 32)
    private DemandType demandType;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    /**
     * 子货号明细（v1.6.0，JSON 数组）。
     * 存储格式：[{"subCode":"be","quantity":100,"destination":"久留米"},...]
     * 旧数据（v1.5.x）：["be","bu","re"] → 反序列化时兼容转成 [{subCode:"be"},{subCode:"bu"},...]
     * 内部字段，get/set 通过 Assembler 操作 JSON 序列化。
     * 注意：使用 TEXT 而非 VARCHAR，避免中文多字节字符超出字节限制。
     */
    @Column(name = "sub_product_code", columnDefinition = "TEXT")
    private String subProductItemsRaw;

    @Column(name = "japan_lead", length = 64)
    private String japanLead;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DemandStatus status = DemandStatus.PENDING;

    /**
     * 关联发注表明细（v1.6.0，JSON 数组）。
     * 存储格式：[{"linkedProcurementId":101,"subCode":"be"},...]
     */
    @Column(name = "linked_demand_items", columnDefinition = "TEXT")
    private String linkedDemandItemsRaw;

    @Column(name = "remarks", length = 512)
    private String remarks;

    // ===== 领域方法 =====

    /** 标记为已转采购（v1.6.0，实际赋值由 UseCase 通过 Assembler 操作） */
    public void markAsConverted() {
        if (this.status != DemandStatus.PENDING) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "demand.already_processed",
                    "需求单已处理，无法转为采购单");
        }
        this.status = DemandStatus.CONVERTED;
    }

    /** 撤销转换（v1.6.0） */
    public void revertConversion() {
        if (this.status != DemandStatus.CONVERTED) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "demand.not_converted",
                    "需求单未转换，无需撤销");
        }
        this.status = DemandStatus.PENDING;
        this.linkedDemandItemsRaw = null;
    }

    /** 取消需求 */
    public void cancel() {
        if (this.status != DemandStatus.PENDING) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "demand.already_processed",
                    "需求单已处理，无法取消");
        }
        this.status = DemandStatus.CANCELLED;
    }
}
