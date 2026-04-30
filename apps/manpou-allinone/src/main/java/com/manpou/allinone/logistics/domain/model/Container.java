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
 */
@Entity
@Table(name = "container", indexes = {
        @Index(name = "uk_container_no", columnList = "container_no", unique = true),
        @Index(name = "idx_container_status", columnList = "status"),
        @Index(name = "idx_container_pool", columnList = "pool_id"),
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
    private LocalDate arrivalDate;              // 到港日期

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
}
