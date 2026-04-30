package com.manpou.allinone.logistics.domain.model;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 拼柜池聚合根（v1.5.0，SPEC-B00 Issue #8）。
 * 同一目的港的多个 LogisticsPlan 可加入同一池，
 * 当 totalCbm >= containerThresholdCbm 时触发装柜。
 */
@Entity
@Table(name = "consolidation_pool", indexes = {
        @Index(name = "uk_pool_code", columnList = "pool_code", unique = true),
        @Index(name = "idx_pool_status", columnList = "status"),
        @Index(name = "idx_pool_destination", columnList = "destination_port"),
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class ConsolidationPool extends BaseEntity {

    @Column(name = "pool_code", nullable = false, unique = true, length = 32)
    private String poolCode;                    // 如 CP-20260430-001

    @Column(name = "destination_port", nullable = false, length = 64)
    private String destinationPort;             // 目的港

    @Column(name = "total_cbm", precision = 12, scale = 4)
    private BigDecimal totalCbm = BigDecimal.ZERO;

    @Column(name = "total_weight_kg", precision = 12, scale = 4)
    private BigDecimal totalWeightKg = BigDecimal.ZERO;

    @Column(name = "plan_count")
    private Integer planCount = 0;

    @Column(name = "container_threshold_cbm", precision = 10, scale = 4)
    private BigDecimal containerThresholdCbm = new BigDecimal("70");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ConsolidationPoolStatus status = ConsolidationPoolStatus.OPEN;

    // ===== 领域方法 =====

    public void addPlan(BigDecimal volumeCbm, BigDecimal weightKg) {
        if (status != ConsolidationPoolStatus.OPEN) {
            throw new BusinessException("logistics.pool_not_open", "池状态非 OPEN，禁止新增计划");
        }
        if (volumeCbm != null) {
            this.totalCbm = this.totalCbm.add(volumeCbm);
        }
        if (weightKg != null) {
            this.totalWeightKg = this.totalWeightKg.add(weightKg);
        }
        this.planCount = (this.planCount == null ? 0 : this.planCount) + 1;
    }

    public void removePlan(BigDecimal volumeCbm, BigDecimal weightKg) {
        if (status == ConsolidationPoolStatus.LOADED || status == ConsolidationPoolStatus.SHIPPED) {
            throw new BusinessException("logistics.pool_cannot_remove", "已装柜/已出港，禁止移除计划");
        }
        if (volumeCbm != null) {
            this.totalCbm = this.totalCbm.subtract(volumeCbm);
        }
        if (weightKg != null) {
            this.totalWeightKg = this.totalWeightKg.subtract(weightKg);
        }
        this.planCount = Math.max(0, this.planCount - 1);
    }

    public boolean isReadyToLoad() {
        return totalCbm.compareTo(containerThresholdCbm) >= 0;
    }

    public void closeToPending() {
        if (status != ConsolidationPoolStatus.OPEN) {
            throw new BusinessException("logistics.pool_cannot_close", "池状态非 OPEN，禁止封池");
        }
        this.status = ConsolidationPoolStatus.PENDING;
    }

    public void markLoaded() {
        if (status != ConsolidationPoolStatus.OPEN && status != ConsolidationPoolStatus.PENDING) {
            throw new BusinessException("logistics.pool_invalid_load", "池状态不允许装柜");
        }
        this.status = ConsolidationPoolStatus.LOADED;
    }

    public void markShipped() {
        if (status != ConsolidationPoolStatus.LOADED) {
            throw new BusinessException("logistics.pool_invalid_ship", "池状态不允许出港");
        }
        this.status = ConsolidationPoolStatus.SHIPPED;
    }

    public void advanceStatus(ConsolidationPoolStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new BusinessException("logistics.pool_invalid_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }
}
