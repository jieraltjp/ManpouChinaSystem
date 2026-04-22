package com.manpou.allinone.factory.infrastructure.persistence.jpa;

import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 */
@Repository
public interface FactoryJpaAdapter extends FactoryRepository, org.springframework.data.jpa.repository.JpaRepository<Factory, Long> {

    Optional<Factory> findByIdAndIsDeletedFalse(Long id);

    Optional<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName);

    Page<Factory> findByStatusAndIsDeletedFalse(FactoryStatus status, Pageable pageable);

    Page<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName, Pageable pageable);

    @Override
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Factory f WHERE f.isDeleted = false")
    boolean existsByIsDeletedFalse();
}
