package com.manpou.allinone.order.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 发注单快照（ProcurementSnapshot）。
 * 记录下单时刻的工厂和商品信息，保证历史订单数据不变。
 * 由 ProcurementUseCase 在创建发注单时自动填充，允许事后修改。
 */
@Entity
@Table(name = "procurement_snapshot")
@Getter
@Setter
public class ProcurementSnapshot extends BaseEntity {

    @Column(name = "procurement_id", nullable = false, unique = true)
    private Long procurementId;

    // ====== 工厂快照（下单时刻）======
    @Column(name = "factory_id")
    private Long factoryId;

    @Column(name = "factory_code", length = 32)
    private String factoryCode;

    @Column(name = "factory_name", length = 128)
    private String factoryName;

    @Column(name = "factory_province", length = 64)
    private String factoryProvince;

    @Column(name = "factory_city", length = 64)
    private String factoryCity;

    @Column(name = "factory_contact_name", length = 64)
    private String factoryContactName;

    @Column(name = "factory_contact_phone", length = 32)
    private String factoryContactPhone;

    // ====== 商品快照（下单时刻）======
    @Column(name = "product_name_zh", length = 255)
    private String productNameZh;

    @Column(name = "product_name_ja", length = 128)
    private String productNameJa;

    @Column(name = "product_category", length = 32)
    private String productCategory;
}
