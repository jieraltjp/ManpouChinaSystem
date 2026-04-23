package com.manpou.allinone.warehouse.domain.repository;

import com.manpou.allinone.warehouse.domain.model.WarehouseExample;
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
public interface WarehouseRepository extends JpaRepository<WarehouseExample, Long> {

    Optional<WarehouseExample> findByNameAndDeletedIsFalse(String name);

    Optional<WarehouseExample> findByIdAndDeletedIsFalse(Long id);

    List<WarehouseExample> findAllByDeletedIsFalse();

    Page<WarehouseExample> findAllByDeletedIsFalse(Pageable pageable);
}
