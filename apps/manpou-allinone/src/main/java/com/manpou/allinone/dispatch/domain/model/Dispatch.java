package com.manpou.allinone.dispatch.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cargo_dispatch", indexes = {
        @Index(name = "idx_dispatch_code", columnList = "code"),
        @Index(name = "idx_dispatch_destination", columnList = "destination"),
        @Index(name = "idx_dispatch_is_deleted", columnList = "is_deleted"),
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Dispatch extends BaseEntity {

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "manager", nullable = false, columnDefinition = "TEXT")
    private String manager;

    @Column(name = "destination", nullable = false, columnDefinition = "TEXT")
    private String destination;

    @Column(name = "tax", nullable = false, columnDefinition = "TEXT")
    private String tax;

    @Column(name = "material", nullable = false, columnDefinition = "TEXT")
    private String material;

    @Column(name = "kensa", length = 255)
    private String kensa;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "pieces", nullable = false)
    private Integer pieces;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "weight2", nullable = false)
    private Double weight2;

    @Column(name = "length", nullable = false)
    private Double length;

    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    private String location;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private String status;

    @Column(name = "other", nullable = false, columnDefinition = "TEXT")
    private String other;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "warehouse", nullable = false, columnDefinition = "TEXT")
    private String warehouse;

    @Column(name = "factory_addr", length = 255)
    private String factoryAddr;

    @Column(name = "show_flag", nullable = false)
    private Integer showFlag = 0;

    @Column(name = "rireki", columnDefinition = "LONGTEXT")
    private String rireki;
}