package com.manpou.allinone.sales.domain.model;

/**
 * 销售渠道枚举。
 * 与前端 apps/web/src/api/salesOperations.ts SalesChannel 完全对齐。
 */
public enum SalesChannel {
    AMAZON,     // Amazon 销售平台
    MERCALI,   // メルカリ二手平台
    SELF_SITE, // 自社サイト自有网站
    OTHER      // 其他渠道
}
