package com.manpou.allinone.replenishment.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 需求-采购分配映射实体（SPEC-B11 §6.2）。
 * 支持一个需求单关联多个采购单（N:1 via DemandProcurementMapping）。
 *
 * 约束：demand.subProductCode == procurement.subProductCode 时才允许创建。
 */
@Entity
@Table(name = "demand_procurement_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uk_demand_procurement", columnNames = "demand_id, procurement_id"),
        indexes = {
                @Index(name = "idx_mapping_demand", columnList = "demand_id"),
                @Index(name = "idx_mapping_procurement", columnList = "procurement_id")
        })
@Access(AccessType.FIELD)
@Getter
@Setter
public class DemandProcurementMapping extends BaseEntity {

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @Column(name = "procurement_id", nullable = false)
    private Long procurementId;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MappingStatus status = MappingStatus.进行中;

    // ===== 领域方法 =====

    /** 终态检查。 */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * 推进为已完成。
     */
    public void markCompleted() {
        if (this.status != MappingStatus.进行中) {
            return;
        }
        this.status = MappingStatus.已完成;
    }

    /**
     * 取消分配。
     */
    public void cancel() {
        if (isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "mapping.cannot_modify_closed",
                    "终态禁止状态变更");
        }
        this.status = MappingStatus.已取消;
    }
}
