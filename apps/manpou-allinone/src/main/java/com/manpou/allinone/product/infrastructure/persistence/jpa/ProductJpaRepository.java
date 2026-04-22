package com.manpou.allinone.product.infrastructure.persistence.jpa;

import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 * 表名: product（由 @Table(name="product") 指定）
 */
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> {

    Optional<Product> findByMasterCodeAndIsDeletedFalse(String masterCode);

    Optional<Product> findByMasterCodeAndSubCodeAndIsDeletedFalse(String masterCode, String subCode);

    Page<Product> findByMasterCodeAndIsDeletedFalse(String masterCode, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.nameZh LIKE %:keyword% AND p.isDeleted = false")
    Page<Product> findByNameZhContainingAndIsDeletedFalse(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.hsCode = :hsCode AND p.isDeleted = false")
    Page<Product> findByHsCodeAndIsDeletedFalse(@Param("hsCode") String hsCode, Pageable pageable);

    List<Product> findByMasterCodeAndIsDeletedFalse(String masterCode);
}
