package com.manpou.allinone.replenishment.domain.model;

import com.manpou.allinone.procurement.domain.model.BaseEntity;
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
 * 补货需求单实体。
 * 对应业务流第一步入口：非新品补货 or 新品采购。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md §2 聚合根 完全对齐。
 */
@Entity
@Table(name = "replenishment_demand", indexes = {
        @Index(name = "idx_demand_status", columnList = "status"),
        @Index(name = "idx_demand_type", columnList = "demand_type"),
        @Index(name = "idx_demand_product_code", columnList = "product_code"),
        @Index(name = "idx_demand_linked_procurement", columnList = "linked_procurement_id")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class ReplenishmentDemand extends BaseEntity {

    @Column(name = "demand_code", unique = true, nullable = false, length = 32)
    private String demandCode;          // 需求编号（系统流水号：DM-YYYYMMDD-NNN）

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type", nullable = false, length = 32)
    private DemandType demandType;    // 需求类型

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;       // 主货号

    @Column(name = "sub_product_code", length = 512)
    private String subProductCode;  // 子货号/枝番（颜色），存储 JSON 数组或单个字符串

    @Column(name = "quantity", nullable = false)
    private Integer quantity;        // 需求量

    @Column(name = "destination", length = 128)
    private String destination;      // 目的地

    @Column(name = "japan_lead", length = 64)
    private String japanLead;       // 日本担当

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DemandStatus status = DemandStatus.PENDING;

    @Column(name = "linked_procurement_id")
    private Long linkedProcurementId; // 关联采购单ID（status=CONVERTED 时赋值）

    @Column(name = "remarks", length = 512)
    private String remarks;         // 备注

    // ===== 领域方法 =====

    /** 转为采购单 */
    public void convertToProcurement(Long procurementId) {
        if (this.status != DemandStatus.PENDING) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "demand.already_processed",
                    "需求单已处理，无法转为采购单");
        }
        this.status = DemandStatus.CONVERTED;
        this.linkedProcurementId = procurementId;
    }

    /**
     * 撤销转换。
     * 适用于：发注单被删除后，需求单需重新转采购。
     * 条件：状态必须为 CONVERTED。
     */
    public void revertConversion() {
        if (this.status != DemandStatus.CONVERTED) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "demand.not_converted",
                    "需求单未转换，无需撤销");
        }
        this.status = DemandStatus.PENDING;
        this.linkedProcurementId = null;
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
