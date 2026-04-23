package com.manpou.allinone.product.domain.repository;

import com.manpou.allinone.product.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓库接口（领域层契约）。
 * JPA 实现在 infrastructure.persistence.jpa.ProductJpaRepository。
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedIsFalse(Long id);

    Optional<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);

    Optional<Product> findByMasterCodeAndSubCodeAndDeletedIsFalse(String masterCode, String subCode);

    Page<Product> findByMasterCodeAndDeletedIsFalse(String masterCode, Pageable pageable);

    Page<Product> findByNameZhContainingAndDeletedIsFalse(String keyword, Pageable pageable);

    Page<Product> findByHsCodeAndDeletedIsFalse(String hsCode, Pageable pageable);

    List<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);

    List<String> findDistinctMasterCodeByKeyword(String keyword);

    List<Object[]> findMasterCodeSuggestions(String keyword);

    List<Object[]> findSubCodesByMasterCode(String masterCode);

    Page<Product> findAllByDeletedIsFalse(Pageable pageable);
}
