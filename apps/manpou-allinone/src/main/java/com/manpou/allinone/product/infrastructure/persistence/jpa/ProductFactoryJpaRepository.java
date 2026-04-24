package com.manpou.allinone.product.infrastructure.persistence.jpa;

import com.manpou.allinone.product.domain.model.ProductFactory;
import com.manpou.allinone.product.domain.repository.ProductFactoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** 批量查询商品关联工厂数量（用于表格列展示） */
    @Query("SELECT pf.productId, COUNT(pf) FROM ProductFactory pf WHERE pf.productId IN :productIds GROUP BY pf.productId")
    List<Object[]> countFactoriesByProductIds(@Param("productIds") List<Long> productIds);

    /** 批量查询商品关联工厂名称（逗号分隔，用于表格列展示） */
    @Query(value = """
        SELECT pf.product_id,
               GROUP_CONCAT(DISTINCT f.factory_name ORDER BY pf.is_preferred DESC, f.factory_name SEPARATOR ',')
        FROM product_factory pf
        JOIN factory f ON f.id = pf.factory_id AND f.is_deleted = FALSE
        WHERE pf.product_id IN :productIds
        GROUP BY pf.product_id
        """, nativeQuery = true)
    List<Object[]> findFactoryNamesByProductIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT COUNT(DISTINCT pf.factoryId) FROM ProductFactory pf")
    long countFactoriesWithLinkedProducts();
}
