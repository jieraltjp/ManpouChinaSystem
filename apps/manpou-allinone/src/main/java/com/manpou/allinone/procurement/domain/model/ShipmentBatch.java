package com.manpou.allinone.procurement.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 出货批次聚合根（SPEC-B11 §6.1）。
 * 记录厂家每次发货事件。
 *
 * 设计：ShipmentBatch 不持有 QcRecord 列表引用，
 * 关联方向为 QcRecord → ShipmentBatch（FK: shipmentBatchId）。
 * 这是行业标准做法（SAP QM、金蝶、用友均如此）。
 */
@Entity
@Table(name = "shipment_batch", indexes = {
        @Index(name = "idx_sb_procurement", columnList = "procurement_id"),
        @Index(name = "idx_sb_status", columnList = "status")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class ShipmentBatch extends BaseEntity {

    @Column(name = "procurement_id", nullable = false)
    private Long procurementId;

    @Column(name = "batch_code", nullable = false, length = 32)
    private String batchCode;

    @Column(name = "shipment_quantity", nullable = false)
    private Integer shipmentQuantity;

    @Column(name = "factory_ship_date")
    private LocalDate factoryShipDate;

    @Column(name = "actual_ship_date")
    private LocalDate actualShipDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ShipmentBatchStatus status = ShipmentBatchStatus.待验货;

    @Column(name = "remarks", length = 512)
    private String remarks;

    // ===== 领域方法 =====

    /** 终态检查。 */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * 推进状态（FSM）。
     */
    public void updateStatus(ShipmentBatchStatus newStatus) {
        if (isTerminal()) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "shipment_batch.cannot_modify_closed",
                    "终态禁止状态变更");
        }
        if (!status.canTransitionTo(newStatus)) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "shipment_batch.invalid_status_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }

    /**
     * 当第一条验货记录关联到本批次时调用，推进状态为验货中。
     */
    public void advanceStatusOnQcLink() {
        if (status == ShipmentBatchStatus.待验货) {
            updateStatus(ShipmentBatchStatus.验货中);
        }
    }

    /**
     * 当所有关联的 QcRecord 都 COMPLETED 时，推进状态为已验货。
     */
    public void advanceStatusOnAllQcCompleted() {
        if (status == ShipmentBatchStatus.验货中) {
            updateStatus(ShipmentBatchStatus.已验货);
        }
    }

    /**
     * 人工取消。
     */
    public void cancel() {
        if (status == ShipmentBatchStatus.已验货) {
            throw new com.manpou.allinone.common.exception.BusinessException(
                    "shipment_batch.cannot_cancel_completed",
                    "已验货状态不允许取消");
        }
        updateStatus(ShipmentBatchStatus.已取消);
    }
}
