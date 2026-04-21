package com.manpou.allinone.logistics.domain.model;

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

import java.math.BigDecimal;

/**
 * 调配计划聚合根。
 * 对应业务流第四步。
 */
@Entity
@Table(name = "logistics_plan", indexes = {
        @Index(name = "uk_plan_code", columnList = "plan_code", unique = true),
        @Index(name = "idx_logistics_procurement", columnList = "procurement_id"),
        @Index(name = "idx_logistics_status", columnList = "status"),
        @Index(name = "idx_logistics_plan_type", columnList = "plan_type"),
        @Index(name = "idx_logistics_factory", columnList = "factory_id")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class LogisticsPlan extends BaseEntity {

    @Column(name = "plan_code", nullable = false, unique = true, length = 32)
    private String planCode;              // 系统流水号，如 L-20260421-001

    @Column(name = "procurement_id")
    private Long procurementId;          // 关联采购单（拼柜时可为空）

    @Column(name = "factory_id")
    private Long factoryId;              // 关联工厂ID

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;          // 货号

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;        // 子货号/颜色

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;            // 调配类型

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private LogisticsStatus status = LogisticsStatus.PLANNED;

    // 货物尺寸
    @Column(name = "cargo_length_cm", precision = 8, scale = 2)
    private BigDecimal cargoLengthCm;     // 长(cm)

    @Column(name = "cargo_width_cm", precision = 8, scale = 2)
    private BigDecimal cargoWidthCm;      // 宽(cm)

    @Column(name = "cargo_height_cm", precision = 8, scale = 2)
    private BigDecimal cargoHeightCm;     // 高(cm)

    @Column(name = "cargo_weight_kg", precision = 10, scale = 4)
    private BigDecimal cargoWeightKg;     // 重量(kg)

    @Column(name = "cargo_volume_cbm", precision = 10, scale = 6)
    private BigDecimal cargoVolumeCbm;     // 体积(m³)

    private Integer quantity;            // 数量

    @Column(name = "requires_qc")
    private Boolean requiresQc;           // 是否需要检测

    @Column(name = "container_id")
    private Long containerId;            // 货柜ID（装柜后赋值）

    @Column(name = "pool_id")
    private Long poolId;                  // 拼柜池ID

    @Column(name = "estimated_ship_date")
    private java.time.LocalDate estimatedShipDate;  // 预计发货日

    @Column(name = "actual_ship_date")
    private java.time.LocalDate actualShipDate;   // 实际发货日

    @Column(name = "remarks", length = 512)
    private String remarks;               // 备注

    private static final BigDecimal CM3_TO_M3 = new BigDecimal("1000000");

    // ===== 领域方法 =====

    /** 计算体积(m³)。 */
    public void calculateVolume() {
        if (cargoLengthCm != null && cargoWidthCm != null && cargoHeightCm != null) {
            this.cargoVolumeCbm = cargoLengthCm
                    .multiply(cargoWidthCm)
                    .multiply(cargoHeightCm)
                    .divide(CM3_TO_M3, 6, java.math.RoundingMode.HALF_UP);
        }
    }

    /** 终态检查。 */
    public boolean isTerminal() {
        return status == LogisticsStatus.DELIVERED;
    }

    /** 推进状态。 */
    public void updateStatus(LogisticsStatus newStatus) {
        if (isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "logistics.cannot_modify_delivered",
                    "已完成状态禁止任何变更");
        }
        if (!status.canTransitionTo(newStatus)) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "logistics.invalid_status_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }
}
