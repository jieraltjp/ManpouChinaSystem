package com.manpou.allinone.procurement.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 发注单实体（对应 Excel 出货单弹窗）。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §2 聚合根 完全对齐。
 */
@Entity
@Table(name = "procurement", indexes = {
        @Index(name = "idx_procurement_product_code", columnList = "product_code"),
        @Index(name = "idx_procurement_status", columnList = "status"),
        @Index(name = "idx_procurement_create_time", columnList = "create_time"),
        @Index(name = "idx_procurement_factory_id", columnList = "factory_id"),
        @Index(name = "idx_procurement_sub_product_code", columnList = "sub_product_code"),
        @Index(name = "idx_procurement_order_date", columnList = "order_date")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Procurement extends BaseEntity {

    private static final BigDecimal CONSOLIDATION_FEE = new BigDecimal("1.02");
    private static final BigDecimal PROFIT_MARGIN     = new BigDecimal("1.2");
    private static final BigDecimal EXCHANGE_BUFFER  = new BigDecimal("1.05");

    // ===== 关联 =====
    @Column(name = "factory_id")
    private Long factoryId;             // 关联工厂ID（FK → factory.id）

    // ===== 商品信息 =====
    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;         // 主货号（待关联 Product.masterCode）

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;     // 子货号/枝番（颜色，如 re/wh/bk）

    @Column(name = "material", length = 64)
    private String material;            // 材质

    @Column(name = "requires_qc")
    private Boolean requiresQc;        // 是否需要检测

    @Column(name = "quantity", nullable = false)
    private Integer quantity;          // 订购数量

    // ===== 价格信息 =====
    @Column(name = "price_rmb", nullable = false, precision = 12, scale = 4)
    private BigDecimal priceRmb;       // 人民币单价

    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate;   // CNY→JPY 汇率

    @Column(name = "tax_point", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxPoint;       // 票点（默认 1.1）

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", length = 32)
    private BillingType billingType;   // 报关类型（v1.3.0 新增）

    @Column(name = "estimated_price_jpy", precision = 14, scale = 2)
    private BigDecimal estimatedPriceJpy; // 估算批发价 JPY（只读计算字段）

    // ===== 报关与说明 =====
    @Column(name = "customs_remarks", length = 512)
    private String customsRemarks;    // 报关备注（v1.3.0 新增）

    @Column(name = "instruction_manual", columnDefinition = "TEXT")
    private String instructionManual; // 说明书（v1.3.0 新增）

    // ===== 日期 =====
    @Column(name = "order_date")
    private java.time.LocalDate orderDate;       // 下单日

    @Column(name = "factory_ship_date")
    private java.time.LocalDate factoryShipDate; // 厂家出货日

    @Column(name = "planned_ship_date")
    private java.time.LocalDate plannedShipDate; // 计划出货日（交货期）

    @Column(name = "actual_ship_date")
    private java.time.LocalDate actualShipDate;  // 实际出货日（v1.3.0 新增）

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;    // 交货期天数（30/45/60）

    @Column(name = "carton_notes", length = 512)
    private String cartonNotes;     // 纸箱备注（v1.9.0 新增）

    @Column(name = "after_sales_deadline")
    private java.time.LocalDate afterSalesDeadline; // 售后截止日（v1.10.0 新增）

    // ===== 担当 =====
    @Column(name = "product_lead", length = 64)
    private String productLead;        // 商品担当

    @Column(name = "japan_lead", length = 64)
    private String japanLead;          // 日本担当

    @Column(name = "china_lead", length = 64)
    private String chinaLead;          // 中国担当

    // ===== 发货信息 =====
    @Column(name = "destination", length = 128)
    private String destination;        // 发送目的地

    @Column(name = "customer_company", length = 128)
    private String customerCompany;    // 客户公司

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.未定;

    /** 退货原因（标记为订货失败时填写） */
    @Column(name = "return_reason", length = 512)
    private String returnReason;

    /** 退货时间 */
    @Column(name = "return_date")
    private java.time.LocalDateTime returnDate;

    // ===== 领域方法 =====

    /**
     * 计算估算批发价 JPY。
     * 公式：(priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
     */
    public void calculateEstimatedPriceJpy() {
        if (priceRmb == null || taxPoint == null || exchangeRate == null) {
            return;
        }
        BigDecimal base = priceRmb
                .divide(taxPoint, 4, java.math.RoundingMode.HALF_UP)
                .multiply(CONSOLIDATION_FEE)
                .multiply(PROFIT_MARGIN)
                .multiply(exchangeRate)
                .multiply(EXCHANGE_BUFFER);
        this.estimatedPriceJpy = base.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 更新状态（终态禁止修改）。
     * 标记为退货时同时设置退货原因和退货时间。
     */
    public void updateStatus(ShipmentStatus newStatus, String returnReason) {
        if (this.status.isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.cannot_modify_closed",
                    "终态禁止状态变更");
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.invalid_status_transition",
                    String.format("状态「%s」不允许跳转至「%s」", this.status.name(), newStatus.name()));
        }
        this.status = newStatus;
        if (newStatus == ShipmentStatus.退货) {
            this.returnReason = returnReason;
            this.returnDate = java.time.LocalDateTime.now();
        }
    }

    /**
     * 重置为未定。
     */
    public void resetToUndecided() {
        if (this.status.isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.cannot_modify_closed", "终态禁止状态变更");
        }
        this.status = ShipmentStatus.未定;
    }
}
