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
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 发注单实体（对应 Excel 出货单弹窗）。
 * 与 docs/business/SPEC-发注管理流程.md §2.1 完全对齐。
 *
 * 业务含义：一次发注 = 多条商品行 + 状态追踪。
 */
@Entity
@Table(name = "procurement", indexes = {
        @Index(name = "idx_procurement_product_code", columnList = "product_code"),
        @Index(name = "idx_procurement_status", columnList = "status"),
        @Index(name = "idx_procurement_create_time", columnList = "create_time")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Procurement extends BaseEntity {

    // ===== 商品信息 =====
    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;          // 商品代码（关联 Product.productCode）

    @Column(name = "quantity", nullable = false)
    private Integer quantity;             // 订购数量

    @Column(name = "price_rmb", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceRmb;          // 人民币单价

    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate;      // CNY→JPY 汇率

    @Column(name = "tax_point", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxPoint;          // 票点（默认 1.1）

    @Column(name = "billing_method", length = 32)
    private String billingMethod;         // 计费方式（METHOD_A 等）

    // ===== 价格计算结果（只读） =====
    /**
     * 批发价 JPY（自动计算）。
     * 公式：(priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
     * 前端实时计算，后端存储结果。
     */
    @Column(name = "estimated_price_jpy", precision = 14, scale = 2)
    private BigDecimal estimatedPriceJpy; // 估算批发价 JPY

    // ===== 日期 =====
    @Column(name = "order_date")
    private java.time.LocalDate orderDate;        // 下单日（1688下单日期）

    @Column(name = "factory_ship_date")
    private java.time.LocalDate factoryShipDate;  // 厂家出货日

    @Column(name = "planned_ship_date")
    private java.time.LocalDate plannedShipDate;  // 计划出货日

    // ===== 担当 =====
    @Column(name = "product_lead", length = 64)
    private String productLead;          // 商品担当

    @Column(name = "japan_lead", length = 64)
    private String japanLead;             // 日本担当

    @Column(name = "china_lead", length = 64)
    private String chinaLead;             // 中国担当

    // ===== 发货信息 =====
    @Column(name = "destination", length = 128)
    private String destination;           // 发送目的地

    @Column(name = "customer_company", length = 128)
    private String customerCompany;        // 客户公司

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.未定;

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
                .multiply(new BigDecimal("1.02"))
                .multiply(new BigDecimal("1.2"))
                .multiply(exchangeRate)
                .multiply(new BigDecimal("1.05"));
        this.estimatedPriceJpy = base.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** 更新状态（终态禁止修改，非法转换抛出 BusinessException） */
    public void updateStatus(ShipmentStatus newStatus) {
        if (this.status.isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.cannot_modify_closed",
                    "完了状态禁止任何状态变更");
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.invalid_status_transition",
                    String.format("状态「%s」不允许跳转至「%s」", this.status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }

    /** 重置为未定 */
    public void resetToUndecided() {
        if (this.status.isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "business.cannot_modify_closed", "完了状态禁止任何变更");
        }
        this.status = ShipmentStatus.未定;
    }
}
