package com.manpou.allinone.customs.domain.model;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日本清关记录聚合根（步骤6）。
 * 对应 docs/business/SPEC-B06-日本清关-步骤6.md §2.1。
 *
 * 触发时机：DomesticCustomsRecord.status = CLEARED 时自动/手动创建。
 */
@Entity
@Table(name = "japan_customs_record", indexes = {
        @Index(name = "idx_jp_container_no", columnList = "container_no"),
        @Index(name = "idx_jp_procurement_id", columnList = "procurement_id"),
        @Index(name = "idx_jp_domestic_customs_id", columnList = "domestic_customs_id"),
        @Index(name = "idx_jp_logistics_plan_id", columnList = "logistics_plan_id"),
        @Index(name = "idx_jp_status", columnList = "status")
})
@Getter
@Setter
public class JapanCustomsRecord extends BaseEntity {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static long SEQ = System.currentTimeMillis() % 1000;

    @Column(name = "customs_entry_no", length = 64)
    private String customsEntryNo;  // 入境报关号（格式：JC-YYYYMMDD-NNN）

    @Column(name = "container_no", length = 32)
    private String containerNo;     // 货柜号（v1.4.0 必填，与国内报关一致）

    @Column(name = "domestic_customs_id")
    private Long domesticCustomsId; // 关联国内报关单

    @Column(name = "logistics_plan_id")
    private Long logisticsPlanId;   // 关联调配计划

    @Column(name = "procurement_id")
    private Long procurementId;     // 关联采购单（v1.4.0 改为可选参考）

    @Column(name = "factory_id")
    private Long factoryId;        // 关联工厂（v1.4.0 新增）

    @Column(name = "product_code", length = 32)
    private String productCode;    // 货号（v1.4.0 新增）

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;  // 子货号/颜色（来自 Procurement，全链路追踪）

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private JapanCustomsStatus status = JapanCustomsStatus.PENDING;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;  // 到达日期

    @Column(name = "customs_broker", length = 128)
    private String customsBroker;  // 清关行

    @Column(name = "broker_phone", length = 32)
    private String brokerPhone;     // 清关行电话

    @Column(name = "broker_contact", length = 64)
    private String brokerContact;   // 清关行联系人

    @Column(name = "import_duty_paid", precision = 14, scale = 2)
    private BigDecimal importDutyPaid;    // 进口关税（JPY）

    @Column(name = "consumption_tax_paid", precision = 14, scale = 2)
    private BigDecimal consumptionTaxPaid; // 消费税（JPY）

    @Column(name = "clearance_date")
    private LocalDate clearanceDate; // 清关完成日期

    @Column(name = "arrival_port", length = 64)
    private String arrivalPort;     // 目的港

    @Column(name = "declared_weight_kg", precision = 10, scale = 3)
    private BigDecimal declaredWeightKg; // 申报重量（kg）

    @Column(name = "declared_volume_cbm", precision = 10, scale = 4)
    private BigDecimal declaredVolumeCbm; // 申报体积（m³）

    @Column(name = "remarks", length = 512)
    private String remarks;         // 备注

    // ─── 领域方法 ────────────────────────────────────────────────────

    /**
     * 生成报关号（首次保存前调用）。
     */
    public void generateEntryNo() {
        if (this.customsEntryNo != null) return;
        String date = LocalDateTime.now().format(DATE_FMT);
        this.customsEntryNo = String.format("JC-%s-%03d", date, (++SEQ) % 1000);
    }

    public void startClearance() {
        if (!JapanCustomsStatus.PENDING.canTransitionTo(JapanCustomsStatus.IN_PROGRESS)) {
            throw new BusinessException("japan.customs.cannot_start", "当前状态不允许开始清关");
        }
        this.status = JapanCustomsStatus.IN_PROGRESS;
    }

    public void complete(BigDecimal importDuty, BigDecimal consumptionTax, LocalDate clearanceDate) {
        if (!JapanCustomsStatus.IN_PROGRESS.canTransitionTo(JapanCustomsStatus.CLEARED)) {
            throw new BusinessException("japan.customs.cannot_complete", "当前状态不允许完成清关");
        }
        this.importDutyPaid = importDuty;
        this.consumptionTaxPaid = consumptionTax;
        this.clearanceDate = clearanceDate;
        this.status = JapanCustomsStatus.CLEARED;
    }

    public void fail(String reason) {
        if (!JapanCustomsStatus.IN_PROGRESS.canTransitionTo(JapanCustomsStatus.FAILED)) {
            throw new BusinessException("japan.customs.cannot_fail", "当前状态不允许标记失败");
        }
        this.remarks = (this.remarks == null ? "" : this.remarks + "; ") + "失败原因：" + reason;
        this.status = JapanCustomsStatus.FAILED;
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
