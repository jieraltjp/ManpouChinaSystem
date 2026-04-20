package com.manpou.allinone.procurement.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 发注单示例骨架。
 * 领域层核心，禁外部直接操作。
 * TODO Phase A: 替换为真实 ShippingOrder 实体。
 */
@Entity
@Table(name = "procurement_example", indexes = {
        @Index(name = "idx_procurement_create_time", columnList = "create_time"),
        @Index(name = "idx_procurement_update_time", columnList = "update_time")
})
@Access(AccessType.FIELD)
@Getter
public class ProcurementExample extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.未定;

    // ===== 领域方法（禁止外部直接修改字段） =====

    /** 更新名称 */
    public void rename(String newName) {
        this.name = newName;
    }

    /** 更新状态 */
    public void updateStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
    }
}
