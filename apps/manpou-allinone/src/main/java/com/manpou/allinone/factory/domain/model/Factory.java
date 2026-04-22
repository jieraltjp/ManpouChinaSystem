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

import java.math.BigDecimal;

/**
 * 工厂实体。
 * 对应业务流第二步工厂信息。
 * 与 docs/database/DB-10-factory.md 完全对齐。
 */
@Entity
@Table(name = "factory", indexes = {
        @Index(name = "idx_factory_code", columnList = "factory_code"),
        @Index(name = "idx_factory_name", columnList = "factory_name"),
        @Index(name = "idx_factory_category", columnList = "category"),
        @Index(name = "idx_factory_cooperation_status", columnList = "cooperation_status"),
        @Index(name = "idx_factory_province", columnList = "province"),
        @Index(name = "idx_factory_city", columnList = "city"),
        @Index(name = "idx_factory_deleted", columnList = "is_deleted")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Factory extends BaseEntity {

    // ===== 基础信息 =====
    @Column(name = "factory_code", unique = true, nullable = false, length = 32)
    private String factoryCode;     // 工厂编号（F-YYYYMMDD-NNN）

    @Column(name = "factory_name", nullable = false, length = 128)
    private String factoryName;     // 工厂名称

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private FactoryCategory category = FactoryCategory.OTHER;  // 分类

    // ===== 地理信息 =====
    @Column(name = "province", nullable = false, length = 64)
    private String province = "";    // 省

    @Column(name = "city", nullable = false, length = 64)
    private String city = "";        // 市

    @Column(name = "county", nullable = false, length = 64)
    private String county = "";     // 县/区

    @Column(name = "rough_location", length = 500)
    private String roughLocation;   // 详细地址（粗略）

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;   // 经度

    @Column(name = "latitude", precision = 11, scale = 8)
    private BigDecimal latitude;    // 纬度

    // ===== 联系方式 =====
    @Column(name = "contact_name", length = 64)
    private String contactName;     // 联系人姓名

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;    // 手机号

    @Column(name = "contact_wechat", length = 64)
    private String contactWechat;   // 微信号

    @Column(name = "contact_qq", length = 32)
    private String contactQq;        // QQ号

    // ===== 合作信息 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "cooperation_status", nullable = false, length = 32)
    private CooperationStatus cooperationStatus = CooperationStatus.POTENTIAL;  // 合作状态

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 64)
    private PaymentTerms paymentTerms = PaymentTerms.NET_30;  // 账期

    // ===== 备注 =====
    @Column(name = "notes", length = 500)
    private String notes;           // 备注
}
