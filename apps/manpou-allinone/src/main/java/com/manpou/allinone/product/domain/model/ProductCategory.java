package com.manpou.allinone.product.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 商品分类（7项）。
 * OEM/普货/厂家出口 + 普通采购/样品/自用/配件/无关联。
 * 对应 docs/database/DB-11-product.md §3 枚举定义。
 */
@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    OEM,            // OEM 定制产品（批发）
    ORDINARY,      // 普通商品（普货）
    FACTORY_DIRECT, // 工厂直供（厂家出口）
    NORMAL,         // 普通采购
    SAMPLE,         // 样品
    SELF_USE,       // 自用
    PARTS,          // 配件
    INDEPENDENT      // 无关联
}
