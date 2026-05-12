package com.manpou.allinone.logistics.domain.model;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 货柜聚合根（v1.5.0，SPEC-B00 Issue #8）。
 * 由 ConsolidationPool 触发装柜后创建。
 * v2.0 扩展（SPEC-B12）：新增 shipId / timeSlot / arrivalLocation / remarks。
 */
@Entity
@Table(name = "container", indexes = {
        @Index(name = "uk_container_no", columnList = "container_no", unique = true),
        @Index(name = "idx_container_status", columnList = "status"),
        @Index(name = "idx_container_pool", columnList = "pool_id"),
        @Index(name = "idx_container_ship_id", columnList = "ship_id"),
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Container extends BaseEntity {

    @Column(name = "container_no", nullable = false, length = 32)
    private String containerNo;                 // 货柜号（如 TEMU1234567）

    @Enumerated(EnumType.STRING)
    @Column(name = "container_type", nullable = false, length = 24)
    private ContainerType containerType = ContainerType.GP20;

    @Column(name = "total_cbm", precision = 10, scale = 4)
    private BigDecimal totalCbm = BigDecimal.ZERO;

    @Column(name = "total_weight_kg", precision = 12, scale = 4)
    private BigDecimal totalWeightKg = BigDecimal.ZERO;

    @Column(name = "plan_count")
    private Integer planCount = 0;

    @Column(name = "pool_id")
    private Long poolId;                       // 关联拼柜池

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ContainerStatus status = ContainerStatus.CREATED;

    @Column(name = "load_date")
    private LocalDate loadDate;                // 装柜日期

    @Column(name = "departure_date")
    private LocalDate departureDate;           // 离港日期

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;             // 到港日期

    // ===== v2.0 扩展字段（SPEC-B12）=====
    @Column(name = "ship_id")
    private Long shipId;                      // 关联船只（NULL=待配船）

    @Column(name = "time_slot", length = 32)
    private String timeSlot;                  // 时间段，如 2026-W24

    @Column(name = "arrival_location", length = 128)
    private String arrivalLocation;           // 到岗地点/最终送达地址

    @Column(name = "remarks", length = 512)
    private String remarks;                    // 备注

    // ===== 领域方法 =====

    public void addPlan(BigDecimal volumeCbm, BigDecimal weightKg) {
        if (status != ContainerStatus.CREATED) {
            throw new BusinessException("logistics.container_not_editable", "货柜状态非 CREATED，禁止修改");
        }
        if (volumeCbm != null) this.totalCbm = this.totalCbm.add(volumeCbm);
        if (weightKg != null) this.totalWeightKg = this.totalWeightKg.add(weightKg);
        this.planCount = (this.planCount == null ? 0 : this.planCount) + 1;
    }

    public void advanceStatus(ContainerStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new BusinessException("logistics.container_invalid_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }

    /**
     * 分配船只（关联船只后自动推进状态至 LOADED）。
     * v2.0 SPEC-B12
     */
    public void assignShip(Long newShipId, LocalDate loadDate) {
        if (this.shipId != null) {
            throw new BusinessException("logistics.container_already_assigned",
                    "货柜已关联船只，请先解除关联");
        }
        this.shipId = newShipId;
        if (loadDate != null) {
            this.loadDate = loadDate;
        }
        if (this.loadDate != null && this.status == ContainerStatus.CREATED) {
            this.status = ContainerStatus.LOADED;
        }
    }

    /**
     * 解除船只关联（回退至 CREATED）。
     * v2.0 SPEC-B12
     */
    public void unassignShip() {
        if (this.shipId == null) {
            return; // 已无关联，无操作
        }
        if (this.status == ContainerStatus.DEPARTED || this.status == ContainerStatus.ARRIVED) {
            throw new BusinessException("logistics.container_cannot_unassign",
                    "货柜已离港或到港，禁止解除船只关联");
        }
        this.shipId = null;
        this.loadDate = null;
        if (this.status == ContainerStatus.LOADED) {
            this.status = ContainerStatus.CREATED;
        }
    }
}
