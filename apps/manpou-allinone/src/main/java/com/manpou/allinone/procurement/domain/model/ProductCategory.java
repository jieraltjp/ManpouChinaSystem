package com.manpou.allinone.procurement.domain.model;

/**
 * 商品类型枚举。
 * 对应商品分类：OEM定制/普货/厂家出口。
 */
public enum ProductCategory {
    OEM,            // OEM定制产品（批量采购）
    ORDINARY,      // 普货
    FACTORY_DIRECT // 厂家出口
}
