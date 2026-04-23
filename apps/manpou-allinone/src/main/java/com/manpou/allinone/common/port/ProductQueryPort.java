package com.manpou.allinone.common.port;

import com.manpou.allinone.product.domain.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * 跨模块：商品查询接口。
 * 避免 procurement / order 等模块直接依赖 product 模块。
 */
public interface ProductQueryPort {

    Optional<Product> findByMasterCode(String masterCode);

    List<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);
}
