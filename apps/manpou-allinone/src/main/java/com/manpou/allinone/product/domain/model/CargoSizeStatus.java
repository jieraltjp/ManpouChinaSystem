package com.manpou.allinone.product.domain.model;

public enum CargoSizeStatus {
    /** 待处理（默认） */
    PENDING,
    /** 已升格为商品 */
    PROMOTED,
    /** 已废弃 */
    DISCARDED
}
