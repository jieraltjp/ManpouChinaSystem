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

    /** 主货号模糊搜索（去重），用于自动补全 */
    @Query("SELECT DISTINCT p.masterCode FROM Product p WHERE p.masterCode LIKE %:kw% AND p.isDeleted = false")
    List<String> findDistinctMasterCodeByKeyword(@Param("kw") String keyword);

    /** 主货号模糊搜索（含名称和颜色数量），限制返回条数 */
    @Query(value = """
        SELECT p.master_code, p.name_zh,
               (SELECT COUNT(*) FROM product c WHERE c.master_code = p.master_code AND c.is_deleted = 0 AND c.sub_code IS NOT NULL AND c.sub_code != '') as color_count
        FROM product p
        WHERE p.master_code LIKE %:kw% AND p.is_deleted = 0
        GROUP BY p.master_code, p.name_zh
        ORDER BY p.master_code
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> findMasterCodeSuggestions(@Param("kw") String keyword);

    /** 按主货号查询所有子货号候选项（用于多选） */
    @Query("SELECT p.subCode, p.colorName FROM Product p WHERE p.masterCode = :masterCode AND p.isDeleted = false AND p.subCode IS NOT NULL AND p.subCode != '' ORDER BY p.subCode")
    List<Object[]> findSubCodesByMasterCode(@Param("masterCode") String masterCode);
}
