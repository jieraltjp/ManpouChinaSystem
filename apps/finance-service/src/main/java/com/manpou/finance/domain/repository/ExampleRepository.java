package com.manpou.finance.domain.repository;

import com.manpou.finance.domain.model.Example;
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
public interface ExampleRepository extends JpaRepository<Example, Long> {

    Optional<Example> findByNameAndIsDeletedFalse(String name);

    Optional<Example> findByIdAndIsDeletedFalse(Long id);

    List<Example> findAllByIsDeletedFalse();

    Page<Example> findAllByIsDeletedFalse(Pageable pageable);
}
