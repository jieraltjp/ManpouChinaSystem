package com.manpou.allinone.qc.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 验货记录聚合根。
 * 对应业务流第三步。
 */
@Entity
@Table(name = "qc_record", indexes = {
        @Index(name = "uk_qc_code", columnList = "qc_code", unique = true),
        @Index(name = "idx_qc_procurement", columnList = "procurement_id"),
        @Index(name = "idx_qc_result", columnList = "result"),
        @Index(name = "idx_qc_date", columnList = "qc_date"),
        @Index(name = "idx_qc_status", columnList = "status")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class QcRecord extends BaseEntity {

    @Column(name = "qc_code", nullable = false, unique = true, length = 32)
    private String qcCode;                 // 系统流水号，如 Q-20260421-001

    @Column(name = "procurement_id", nullable = false)
    private Long procurementId;            // 关联采购单

    @Column(name = "seller_name", length = 128)
    private String sellerName;            // 卖家名称（来自工厂名）

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;            // 货号

    @Column(name = "sub_product_code", length = 64)
    private String subProductCode;        // 子货号/颜色

    @Column(name = "qc_user_id")
    private Long qcUserId;               // 验货负责人

    @Column(name = "qc_type", length = 16)
    @Enumerated(EnumType.STRING)
    private QcType qcType;               // 验货方式

    @Column(name = "qc_date")
    private java.time.LocalDate qcDate;  // 验货日期

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private QcResult result = QcResult.PASS;  // 验货结果

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private QcStatus status = QcStatus.PENDING;  // 状态

    @Column(name = "inspection_count")
    private Integer inspectionCount;      // 检品数

    @Column(name = "passed_count")
    private Integer passedCount;         // 合格数量

    @Column(name = "defective_count")
    private Integer defectiveCount;       // 不良数量（自动计算）

    @Column(name = "box_count")
    private Integer boxCount;             // 箱数

    @Column(name = "box_length_cm", precision = 8, scale = 2)
    private BigDecimal boxLengthCm;     // 箱子长(cm)

    @Column(name = "box_width_cm", precision = 8, scale = 2)
    private BigDecimal boxWidthCm;      // 箱子宽(cm)

    @Column(name = "box_height_cm", precision = 8, scale = 2)
    private BigDecimal boxHeightCm;     // 箱子高(cm)

    @Column(name = "net_weight_per_unit", precision = 10, scale = 4)
    private BigDecimal netWeightPerUnit; // 单个净重(kg)

    @Column(name = "gross_weight", precision = 10, scale = 4)
    private BigDecimal grossWeight;     // 毛重(kg)

    @Column(name = "tax_inclusive_price", precision = 14, scale = 2)
    private BigDecimal taxInclusivePrice;  // 含税价（元）

    @Column(name = "material", length = 64)
    private String material;             // 材质

    @Column(name = "tax_refund")
    private Boolean taxRefund;           // 是否退税

    @Column(name = "qc_standard", length = 512)
    private String qcStandard;          // 验收标准

    @Column(name = "remarks", length = 512)
    private String remarks;              // 备注

    @Column(name = "images", columnDefinition = "JSON")
    private String images;               // 缺陷照片URL列表（JSON数组）

    @Column(name = "destination", length = 128)
    private String destination;         // 目的地

    @Column(name = "quantity")
    private Integer quantity;           // 订购数量

    @Column(name = "order_date")
    private java.time.LocalDate orderDate;  // 下单日

    // ===== 领域方法 =====

    /** 终态检查。 */
    public boolean isTerminal() {
        return status == QcStatus.COMPLETED || status == QcStatus.RETURN_REQUESTED;
    }

    /** 推进状态。 */
    public void updateStatus(QcStatus newStatus) {
        if (isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "qc.cannot_modify_completed",
                    "终态禁止状态变更");
        }
        if (!status.canTransitionTo(newStatus)) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "qc.invalid_status_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }

    /** 计算不良数。 */
    public void calculateDefectiveCount() {
        if (inspectionCount != null && passedCount != null && inspectionCount >= passedCount) {
            this.defectiveCount = inspectionCount - passedCount;
        }
    }
}
