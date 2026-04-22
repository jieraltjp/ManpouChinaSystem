package com.manpou.allinone.product.domain.repository;

import com.manpou.allinone.product.domain.model.ProductFactory;

import java.util.List;
import java.util.Optional;

/**
 * 商品-工厂关联仓库接口（领域层契约）。
 * 对应 docs/business/SPEC-B10 §2.2。
 */
public interface ProductFactoryRepository {

    Optional<ProductFactory> findById(Long id);

    ProductFactory save(ProductFactory entity);

    void deleteById(Long id);

    List<ProductFactory> findByProductId(Long productId);

    List<ProductFactory> findByFactoryId(Long factoryId);

    Optional<ProductFactory> findByProductIdAndFactoryId(Long productId, Long factoryId);

    void deleteByProductIdAndFactoryId(Long productId, Long factoryId);

    boolean existsByProductIdAndFactoryId(Long productId, Long factoryId);
}
