package com.manpou.allinone.logistics.domain.model;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 货柜聚合根（SPEC-B14：list7 历史数据管理）。
 */
@Entity
@Table(name = "container", indexes = {
        @Index(name = "idx_container_status", columnList = "status"),
        @Index(name = "idx_container_ship_id", columnList = "ship_id"),
        @Index(name = "idx_container_show_flag", columnList = "show_flag"),
        @Index(name = "idx_container_cabinet_no", columnList = "cabinet_no"),
        @Index(name = "idx_container_legacy_id", columnList = "legacy_id"),
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Container extends BaseEntity {

    @Column(name = "container_no", nullable = false, length = 32)
    private String containerNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ContainerStatus status = ContainerStatus.CREATED;

    @Column(name = "load_date")
    private LocalDate loadDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "ship_id")
    private Long shipId;

    @Column(name = "time_slot", length = 32)
    private String timeSlot;

    @Column(name = "arrival_location", length = 128)
    private String arrivalLocation;

    @Column(name = "remarks", length = 512)
    private String remarks;

    @Column(name = "cabinet_no", length = 16)
    private String cabinetNo;

    @Column(name = "period", length = 16)
    private String period;

    @Column(name = "legacy_status", length = 32)
    private String legacyStatus;

    @Column(name = "show_flag", nullable = false)
    private Boolean showFlag = true;

    @Column(name = "legacy_id")
    private Long legacyId;

    @Column(name = "legacy_updater", length = 64)
    private String legacyUpdater;

    @Column(name = "legacy_updatetime")
    private LocalDateTime legacyUpdatetime;

    @Column(name = "ship_name", length = 64)
    private String shipName;

    public void advanceStatus(ContainerStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new BusinessException("logistics.container_invalid_transition",
                    String.format("状态「%s」不允许跳转至「%s」", status.name(), newStatus.name()));
        }
        this.status = newStatus;
    }

    public void assignShip(Long newShipId, LocalDate loadDate) {
        if (this.shipId != null) {
            throw new BusinessException("logistics.container_already_assigned",
                    "货柜已关联船只，请先解除关联");
        }
        this.shipId = newShipId;
        if (loadDate != null) {
            this.loadDate = loadDate;
        }
        if (this.loadDate != null && this.status == ContainerStatus.CREATED) {
            this.status = ContainerStatus.LOADED;
        }
    }

    public void unassignShip() {
        if (this.shipId == null) {
            return;
        }
        if (this.status == ContainerStatus.DEPARTED || this.status == ContainerStatus.ARRIVED) {
            throw new BusinessException("logistics.container_cannot_unassign",
                    "货柜已离港或到港，禁止解除船只关联");
        }
        this.shipId = null;
        this.loadDate = null;
        if (this.status == ContainerStatus.LOADED) {
            this.status = ContainerStatus.CREATED;
        }
    }
}
