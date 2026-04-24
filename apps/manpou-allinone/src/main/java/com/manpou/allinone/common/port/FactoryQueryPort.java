package com.manpou.allinone.common.port;

import com.manpou.allinone.factory.domain.model.Factory;
import java.util.Optional;

/**
 * 跨模块：工厂查询接口。
 * 避免 logistics / procurement / qc / order / product 等模块直接依赖 factory 模块。
 */
public interface FactoryQueryPort {

    Optional<Factory> findById(Long id);

    Optional<Factory> findByIdAndDeletedIsFalse(Long id);

    /**
     * 统计关联了商品（product_factory）的工厂数量。
     * 用于 FactoryStatsDTO.active 计数（ACTIVE = 有商品关联）。
     */
    long countFactoriesWithLinkedProducts();
}
