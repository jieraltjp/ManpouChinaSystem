package com.manpou.allinone.logistics.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 船只聚合根（v1.0.0，SPEC-B12）。
 * 记录船名、船号、出发港、目的港，供货柜关联船只时选择。
 */
@Entity
@Table(name = "ship", indexes = {
        @Index(name = "uk_ship_number", columnList = "ship_number", unique = true),
        @Index(name = "idx_ship_arrival_port", columnList = "arrival_port"),
        @Index(name = "idx_ship_is_deleted", columnList = "is_deleted"),
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Ship extends BaseEntity {

    @Column(name = "ship_name", nullable = false, length = 64)
    private String shipName;                    // 船名

    @Column(name = "ship_number", nullable = false, unique = true, length = 32)
    private String shipNumber;                  // 船号/航次号

    @Column(name = "carrier", length = 64)
    private String carrier;                    // 船公司

    @Column(name = "departure_port", length = 64)
    private String departurePort;              // 出发港

    @Column(name = "arrival_port", length = 64)
    private String arrivalPort;                // 目的港
}
