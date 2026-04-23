package com.manpou.allinone.sales.domain.model;

/**
 * 销售记录状态枚举（步骤8）。
 */
public enum SalesStatus {
    LISTED,         // 上架销售中
    LOW_STOCK,      // 库存低于预警值
    OUT_OF_STOCK,   // 库存为零
    DISCONTINUED;   // 已下架（终态）

    public boolean isTerminal() {
        return this == DISCONTINUED;
    }

    public boolean canTransitionTo(SalesStatus target) {
        if (this == target) return false;
        return switch (this) {
            case LISTED -> target == LOW_STOCK || target == OUT_OF_STOCK || target == DISCONTINUED;
            case LOW_STOCK -> target == OUT_OF_STOCK || target == LISTED || target == DISCONTINUED;
            case OUT_OF_STOCK -> target == LISTED || target == DISCONTINUED;
            case DISCONTINUED -> false;
        };
    }

    public boolean isListed() {
        return this == LISTED || this == LOW_STOCK;
    }
}
