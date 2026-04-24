package com.manpou.allinone.customs.domain.repository;

import com.manpou.allinone.customs.domain.model.CustomsExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 示例实体仓库接口（遗留桩，已被 DomesticCustomsRepository 替代）。
 * 仅保留用于避免破坏性修改。
 */
public interface CustomsRepository extends JpaRepository<CustomsExample, Long> {

    Optional<CustomsExample> findByNameAndDeletedIsFalse(String name);

    Optional<CustomsExample> findByIdAndDeletedIsFalse(Long id);
}
