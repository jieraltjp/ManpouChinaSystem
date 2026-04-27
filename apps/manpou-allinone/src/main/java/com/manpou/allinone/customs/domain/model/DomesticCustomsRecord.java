package com.manpou.allinone.customs.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 国内报关记录。
 *
 * Phase 5 领域实体。
 *
 * 状态流转：
 *   PENDING → SUBMITTED → CLEARED | REJECTED
 *   REJECTED 可修正后重新提交。
 *
 * v1.3.0 新增 containerNo 字段，支持货柜级聚合。
 * 创建方式：由用户在 LogisticsPage 点击「创建报关」手动发起。
 */
@Entity
@Table(name = "domestic_customs_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_domestic_customs_code", columnNames = "customs_code"),
        indexes = {
                @Index(name = "idx_dc_container_no", columnList = "container_no"),
                @Index(name = "idx_dc_procurement_id", columnList = "procurement_id"),
                @Index(name = "idx_dc_logistics_plan_id", columnList = "logistics_plan_id"),
                @Index(name = "idx_dc_factory_id", columnList = "factory_id"),
                @Index(name = "idx_dc_status", columnList = "status")
        })
@Access(AccessType.FIELD)
@Getter
@Setter
public class DomesticCustomsRecord extends BaseEntity {

    @Column(name = "customs_code", nullable = false, unique = true, length = 32)
    private String customsCode;           // 系统流水号，如 DC-20260421-001

    @Column(name = "container_no", length = 32)
    private String containerNo;           // 货柜号（v1.3.0，来自 LogisticsPlan.containerNo）

    @Column(name = "procurement_id")
    private Long procurementId;           // 关联发注单

    @Column(name = "logistics_plan_id")
    private Long logisticsPlanId;         // 触发来源调配计划

    @Column(name = "factory_id")
    private Long factoryId;               // 关联工厂

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;           // 货号

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;       // 子货号

    @Column(name = "quantity")
    private Integer quantity;             // 报关数量

    @Column(name = "estimated_value_cny", precision = 14, scale = 2)
    private java.math.BigDecimal estimatedValueCny;  // 预估货值（元）

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private DomesticCustomsStatus status = DomesticCustomsStatus.PENDING;

    @Column(name = "remarks", length = 512)
    private String remarks;               // 备注

    // ===== 领域方法 =====

    public boolean isTerminal() {
        return status == DomesticCustomsStatus.CLEARED;
    }

    public void submit() {
        if (isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "customs.already_cleared", "已通关，禁止再次提交");
        }
        this.status = DomesticCustomsStatus.SUBMITTED;
    }

    public void clear() {
        if (this.status != DomesticCustomsStatus.SUBMITTED) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "customs.must_submit_first", "必须先提交才能完成通关");
        }
        this.status = DomesticCustomsStatus.CLEARED;
    }

    public void reject(String reason) {
        if (this.status == DomesticCustomsStatus.CLEARED) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "customs.cannot_reject_cleared", "已通关记录无法驳回");
        }
        this.status = DomesticCustomsStatus.REJECTED;
        this.remarks = (this.remarks == null ? "" : this.remarks + "; ") + "驳回原因: " + reason;
    }
}
