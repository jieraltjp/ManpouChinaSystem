package com.manpou.allinone.product.domain.repository;

import com.manpou.allinone.product.domain.model.ProductExample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 示例实体仓库接口。
 * 定义领域层需要的查询方法（领域接口）。
 * JPA 实现在 infrastructure.persistence 包。
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductExample, Long> {

    Optional<ProductExample> findByNameAndIsDeletedFalse(String name);

    Optional<ProductExample> findByIdAndIsDeletedFalse(Long id);

    List<ProductExample> findAllByIsDeletedFalse();

    Page<ProductExample> findAllByIsDeletedFalse(Pageable pageable);
}
