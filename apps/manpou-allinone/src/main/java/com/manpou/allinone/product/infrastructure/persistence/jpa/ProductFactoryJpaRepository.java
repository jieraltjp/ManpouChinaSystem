package com.manpou.allinone.product.infrastructure.persistence.jpa;

import com.manpou.allinone.product.domain.model.ProductFactory;
import com.manpou.allinone.product.domain.repository.ProductFactoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 * 表名: product_factory（由 @Table(name="product_factory") 指定）
 */
@Repository
public interface ProductFactoryJpaRepository extends ProductFactoryRepository, JpaRepository<ProductFactory, Long> {

    List<ProductFactory> findByProductId(Long productId);

    List<ProductFactory> findByFactoryId(Long factoryId);

    Optional<ProductFactory> findByProductIdAndFactoryId(Long productId, Long factoryId);

    void deleteByProductIdAndFactoryId(Long productId, Long factoryId);

    boolean existsByProductIdAndFactoryId(Long productId, Long factoryId);
}
