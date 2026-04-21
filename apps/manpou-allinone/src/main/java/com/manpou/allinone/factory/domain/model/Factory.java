package com.manpou.allinone.factory.domain.model;

import com.manpou.allinone.procurement.domain.model.BaseEntity;
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

/**
 * 工厂实体。
 * 对应业务流第二步工厂信息。
 * 与 docs/business/SPEC-发注管理流程.md §3.2 完全对齐。
 */
@Entity
@Table(name = "factory", indexes = {
        @Index(name = "idx_factory_status", columnList = "status"),
        @Index(name = "idx_factory_name", columnList = "factory_name")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Factory extends BaseEntity {

    @Column(name = "factory_code", unique = true, nullable = false, length = 32)
    private String factoryCode;     // 工厂编号（F-YYYYMMDD-NNN）

    @Column(name = "factory_name", nullable = false, length = 128)
    private String factoryName;     // 工厂名称

    @Column(name = "location", length = 128)
    private String location;        // 工厂位置（省/市）

    @Column(name = "rough_location", length = 128)
    private String roughLocation;  // 粗略位置（工业区/镇/园区）

    @Column(name = "contact_name", length = 64)
    private String contactName;    // 联系人名称

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;   // 联系人电话

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FactoryStatus status = FactoryStatus.ACTIVE;
}
