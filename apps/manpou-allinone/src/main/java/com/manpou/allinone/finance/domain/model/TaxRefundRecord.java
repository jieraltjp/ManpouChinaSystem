package com.manpou.allinone.finance.domain.model;

import com.manpou.allinone.finance.domain.model.BaseEntity;
import com.manpou.allinone.procurement.domain.model.BillingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 退税记录聚合根（步骤7）。
 * 对应 docs/business/SPEC-B07-退税-步骤7.md §2.1。
 *
 * 触发时机：JapanCustomsRecord.status = CLEARED 时，
 * 根据 Procurement.billingType 判断是否创建。
 */
@Entity
@Table(name = "tax_refund_record", indexes = {
        @Index(name = "idx_tr_procurement_id", columnList = "procurement_id"),
        @Index(name = "idx_tr_japan_customs_id", columnList = "japan_customs_id"),
        @Index(name = "idx_tr_status", columnList = "status")
})
@Getter
@Setter
public class TaxRefundRecord extends BaseEntity {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static long SEQ = System.currentTimeMillis() % 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_code", length = 32)
    private String refundCode;  // 退税编号（TR-YYYYMMDD-NNN）

    @Column(name = "procurement_id")
    private Long procurementId;     // 关联采购单

    @Column(name = "japan_customs_id")
    private Long japanCustomsId;    // 关联日本清关单

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private TaxRefundStatus status = TaxRefundStatus.APPLYING;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", length = 32)
    private BillingType billingType; // 报关类型

    @Column(name = "price_rmb", precision = 14, scale = 4)
    private BigDecimal priceRmb;    // 单价（来自 Procurement）

    @Column(name = "quantity", precision = 10)
    private Integer quantity;        // 采购数量（来自 Procurement）

    @Column(name = "tax_point", precision = 6, scale = 4)
    private BigDecimal taxPoint;    // 票点（来自 Procurement）

    @Column(name = "estimated_refund_rmb", precision = 14, scale = 4)
    private BigDecimal estimatedRefundRmb; // 理论退税额（自动计算）

    @Column(name = "actual_refund_rmb", precision = 14, scale = 4)
    private BigDecimal actualRefundRmb;   // 实际退税额

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;     // 汇率

    @Column(name = "refund_date")
    private LocalDate refundDate;        // 实际退税日期

    @Column(name = "refund_bank", length = 128)
    private String refundBank;          // 退税银行账户

    @Column(name = "remarks", length = 512)
    private String remarks;             // 备注

    // ─── 领域方法 ────────────────────────────────────────────────────

    /**
     * 生成退税编号。
     */
    public void generateRefundCode() {
        if (this.refundCode != null) return;
        String date = LocalDateTime.now().format(DATE_FMT);
        this.refundCode = String.format("TR-%s-%03d", date, (++SEQ) % 1000);
    }

    /**
     * 计算理论退税额。
     * 公式：priceRmb × quantity × (taxPoint - 1)
     */
    public void calculateEstimatedRefund() {
        if (priceRmb == null || quantity == null || taxPoint == null) return;
        this.estimatedRefundRmb = priceRmb.multiply(BigDecimal.valueOf(quantity))
                .multiply(taxPoint.subtract(BigDecimal.ONE));
    }

    /**
     * 标记退税完成。
     */
    public void complete(BigDecimal actualAmount, LocalDate refundDate, String refundBank) {
        if (!TaxRefundStatus.APPLYING.canTransitionTo(TaxRefundStatus.COMPLETED)) {
            throw new IllegalStateException("当前状态不允许标记完成");
        }
        this.actualRefundRmb = actualAmount;
        this.refundDate = refundDate;
        this.refundBank = refundBank;
        this.status = TaxRefundStatus.COMPLETED;
    }

    /**
     * 标记为不退税。
     */
    public void markNoRefund() {
        if (!TaxRefundStatus.APPLYING.canTransitionTo(TaxRefundStatus.NO_REFUND)) {
            throw new IllegalStateException("当前状态不允许标记不退税");
        }
        this.status = TaxRefundStatus.NO_REFUND;
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
