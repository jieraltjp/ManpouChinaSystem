package com.manpou.allinone.customs.domain.model;

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
 * 国内报关记录。
 *
 * Phase 5 领域实体。
 *
 * 状态流转：
 *   PENDING → SUBMITTED → CLEARED | REJECTED
 *   REJECTED 可修正后重新提交。
 *
 * 生命周期由 LogisticsPlan.status=IN_TRANSIT 事件自动触发创建。
 */
@Entity
@Table(name = "domestic_customs_record", indexes = {
        @Index(name = "uk_domestic_customs_code", columnList = "customs_code", unique = true),
        @Index(name = "idx_domestic_procurement", columnList = "procurement_id"),
        @Index(name = "idx_domestic_logistics", columnList = "logistics_plan_id"),
        @Index(name = "idx_domestic_status", columnList = "status")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class DomesticCustomsRecord extends BaseEntity {

    @Column(name = "customs_code", nullable = false, unique = true, length = 32)
    private String customsCode;           // 系统流水号，如 DC-20260421-001

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
