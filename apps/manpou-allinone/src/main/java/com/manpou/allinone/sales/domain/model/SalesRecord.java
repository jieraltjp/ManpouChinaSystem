package com.manpou.allinone.sales.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 销售记录聚合根（步骤8 运营销售）。
 * 对应 docs/business/SPEC-B08-运营销售-步骤8.md §2.1。
 */
@Entity
@Table(name = "sales_record", indexes = {
        @Index(name = "idx_sr_product_code", columnList = "product_code"),
        @Index(name = "idx_sr_procurement_id", columnList = "procurement_id"),
        @Index(name = "idx_sr_status", columnList = "status"),
        @Index(name = "idx_sr_sales_channel", columnList = "sales_channel")
})
@Getter
@Setter
public class SalesRecord extends BaseEntity {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static long SEQ = System.currentTimeMillis() % 1000;

    @Column(name = "record_code", length = 32)
    private String recordCode;  // 销售记录编号（SA-YYYYMMDD-NNN）

    @Column(name = "procurement_id")
    private Long procurementId;  // 关联采购单

    @Column(name = "product_code", length = 64)
    private String productCode;  // 主货号

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;  // 子货号（颜色）

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", length = 32)
    private SalesChannel salesChannel;  // 销售渠道

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SalesStatus status = SalesStatus.LISTED;

    @Column(name = "listing_date")
    private LocalDate listingDate;  // 上架日期

    @Column(name = "initial_stock", precision = 10)
    private Integer initialStock;  // 初始上架库存

    @Column(name = "current_stock", precision = 10)
    private Integer currentStock;  // 当前库存

    @Column(name = "safety_stock", precision = 10)
    private Integer safetyStock;  // 安全库存阈值

    @Column(name = "sales_quantity", precision = 10)
    private Integer salesQuantity;  // 累计销量

    @Column(name = "returned_quantity", precision = 10)
    private Integer returnedQuantity;  // 退货数量

    @Column(name = "return_rate", precision = 6, scale = 4)
    private BigDecimal returnRate;  // 退货率

    @Column(name = "selling_price_jpy", precision = 14, scale = 2)
    private BigDecimal sellingPriceJpy;  // 实际销售价（JPY）

    @Column(name = "remarks", length = 512)
    private String remarks;  // 备注

    // ─── 领域方法 ────────────────────────────────────────────────────

    public void generateRecordCode() {
        if (this.recordCode != null) return;
        String date = LocalDateTime.now().format(DATE_FMT);
        this.recordCode = String.format("SA-%s-%03d", date, (++SEQ) % 1000);
    }

    public void updateStock(Integer sold, Integer returned) {
        if (this.currentStock == null) return;
        int s = (sold != null ? sold : 0);
        int r = (returned != null ? returned : 0);
        this.currentStock = Math.max(0, this.currentStock + r - s);
        if (this.currentStock == 0) {
            this.status = SalesStatus.OUT_OF_STOCK;
        } else if (this.safetyStock != null && this.currentStock < this.safetyStock) {
            this.status = SalesStatus.LOW_STOCK;
        } else {
            this.status = SalesStatus.LISTED;
        }
    }

    public void recalculateReturnRate() {
        if (this.salesQuantity == null || this.salesQuantity == 0) {
            this.returnRate = BigDecimal.ZERO;
            return;
        }
        int returned = (this.returnedQuantity != null ? this.returnedQuantity : 0);
        this.returnRate = BigDecimal.valueOf(returned)
                .divide(BigDecimal.valueOf(this.salesQuantity), 4, java.math.RoundingMode.HALF_UP);
    }

    public void discontinue() {
        this.status = SalesStatus.DISCONTINUED;
    }

    public void relist() {
        if (!SalesStatus.OUT_OF_STOCK.canTransitionTo(SalesStatus.LISTED)) {
            throw new IllegalStateException("当前状态不允许重新上架");
        }
        this.status = SalesStatus.LISTED;
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
