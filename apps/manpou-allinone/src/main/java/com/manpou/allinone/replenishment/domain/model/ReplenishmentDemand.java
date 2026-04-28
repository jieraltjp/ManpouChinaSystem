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
 * 补货需求单实体（v2.0.0）。
 * 一条记录 = 一个子货号（主货号+子货号 = 商品唯一标识）。
 * 对应业务流第一步入口：非新品补货 or 新品采购。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md v2.0.0 完全对齐。
 */
@Entity
@Table(name = "replenishment_demand", indexes = {
        @Index(name = "idx_demand_status", columnList = "status"),
        @Index(name = "idx_demand_type", columnList = "demand_type"),
        @Index(name = "idx_demand_product_code", columnList = "product_code"),
        @Index(name = "idx_demand_is_deleted", columnList = "is_deleted"),
        @Index(name = "idx_demand_sub_product_code", columnList = "sub_product_code"),
        @Index(name = "idx_demand_quantity", columnList = "quantity"),
        @Index(name = "idx_demand_destination", columnList = "destination")
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

    /** 主货号 */
    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    /**
     * 子货号（v2.0.0）。
     * 格式：主货号-子货号，如 "ad009-be"。
     * 与主货号联合构成商品唯一标识。
     */
    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;

    /**
     * 需求数量（v2.0.0）。
     * 该子货号的需求数量。
     */
    @Column(name = "quantity")
    private Integer quantity;

    /**
     * 目的地（v2.0.0）。
     * 该子货号的出货目的地，如久留米/名古屋/大阪。
     */
    @Column(name = "destination", length = 128)
    private String destination;

    @Column(name = "japan_lead", length = 64)
    private String japanLead;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DemandStatus status = DemandStatus.PENDING;

    /**
     * 关联的 Procurement ID（v2.2.0）。
     * CONFIRMED 时记录对应的 Procurement.id。
     * 取消关联时清空此字段。
     */
    @Column(name = "linked_procurement_id")
    private Long linkedProcurementId;

    @Column(name = "remarks", length = 512)
    private String remarks;

    /**
     * 商品图片URL（v2.1.0）。
     * 反规范化自 product.image_url，查询零成本。
     * Demand 创建时由 UseCase 从 Product 表同步填充。
     */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    // ===== 领域方法 =====

    /**
     * 标记已关联发注单（v2.2.0）。
     * 由 ProcurementPage 关联 Demand 时调用。
     */
    public void markAsLinked(Long procurementId) {
        this.status = DemandStatus.CONFIRMED;
        this.linkedProcurementId = procurementId;
    }

    /**
     * 取消关联（v2.2.0）。
     * 由 DemandPage 点击 CONFIRMED 标签时调用，或由 ProcurementPage 取消关联时调用。
     */
    public void unlinkProcurement() {
        this.status = DemandStatus.PENDING;
        this.linkedProcurementId = null;
    }
}
