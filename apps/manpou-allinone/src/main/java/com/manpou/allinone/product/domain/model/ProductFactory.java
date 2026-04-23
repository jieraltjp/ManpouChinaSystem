package com.manpou.allinone.product.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品-工厂多对多关联实体（非聚合根）。
 * 由 Product 聚合根管理生命周期。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.2。
 */
@Entity
@Table(name = "product_factory",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_factory", columnNames = {"product_id", "factory_id"}),
        indexes = {
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_factory_id", columnList = "factory_id")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class ProductFactory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;           // FK → product.id

    @Column(name = "factory_id", nullable = false)
    private Long factoryId;           // FK → factory.id

    @Column(name = "supplier_sku", length = 64)
    private String supplierSku;       // 供应商内部货号

    @Column(name = "moq")
    private Integer moq = 1;         // 最小起订量

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;     // 交货周期(天)

    @Column(name = "unit_price_rmb", precision = 12, scale = 4)
    private BigDecimal unitPriceRmb; // 该工厂含税单价

    @Column(name = "is_preferred")
    private Boolean isPreferred = false; // 首选供应商

    @Column(name = "create_time", nullable = false, updatable = false)
    private java.time.LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private java.time.LocalDateTime updateTime;
}
