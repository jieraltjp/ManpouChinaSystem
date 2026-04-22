package com.manpou.allinone.product.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 商品分类。
 * 对应 docs/database/DB-11-product.md §3 枚举定义。
 */
@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    OEM,            // OEM 定制产品
    ORDINARY,      // 普通商品
    FACTORY_DIRECT  // 工厂直供
}
