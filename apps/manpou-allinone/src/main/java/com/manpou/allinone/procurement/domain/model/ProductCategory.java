package com.manpou.allinone.procurement.domain.model;

/**
 * 商品分类枚举（7项）。
 * OEM/普货/厂家出口 + 普通采购/样品/自用/配件/无关联。
 * 用于发注单快照记录商品分类。
 */
public enum ProductCategory {
    OEM,            // OEM定制产品（批发）
    ORDINARY,      // 普货
    FACTORY_DIRECT, // 厂家出口
    NORMAL,         // 普通采购
    SAMPLE,         // 样品
    SELF_USE,       // 自用
    PARTS,          // 配件
    INDEPENDENT      // 无关联
}
