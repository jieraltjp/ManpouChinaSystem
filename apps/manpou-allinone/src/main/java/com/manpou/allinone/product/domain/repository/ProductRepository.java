package com.manpou.allinone.product.domain.repository;

import com.manpou.allinone.product.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓库接口（领域层契约）。
 * JPA 实现在 infrastructure.persistence.jpa.ProductJpaRepository。
 * 注意：不加 @Repository，由 ProductJpaRepository 实现以避免重复 Bean。
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedIsFalse(Long id);

    Optional<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);

    /** 查询 master-level 商品（sub_code IS NULL），用于 /code/{masterCode} 接口 */
    Optional<Product> findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse(String masterCode);

    Optional<Product> findByMasterCodeAndSubCodeAndDeletedIsFalse(String masterCode, String subCode);

    Page<Product> findByMasterCodeAndDeletedIsFalse(String masterCode, Pageable pageable);

    Page<Product> findByNameZhContainingAndDeletedIsFalse(String keyword, Pageable pageable);

    Page<Product> findByHsCodeAndDeletedIsFalse(String hsCode, Pageable pageable);

    Page<Product> findByHsCodeJpAndDeletedIsFalse(String hsCodeJp, Pageable pageable);

    List<Product> findAllByMasterCodeAndDeletedIsFalse(String masterCode);

    List<Product> findAllByMasterCodeInAndDeletedIsFalse(List<String> masterCodes);

    Page<Product> findAllByDeletedIsFalse(Pageable pageable);
}
