package com.manpou.allinone.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品摘要 VO（批量查询用，替代逐个 GET /code/{masterCode} 调用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryVO {

    /** 主货号 */
    private String masterCode;

    /** 商品类别：OEM / ORDINARY / FACTORY_DIRECT */
    private String category;

    /** 商品图片 URL */
    private String imageUrl;
}
