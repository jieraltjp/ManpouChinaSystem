package com.manpou.allinone.factory.infrastructure.persistence.jpa;

import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 */
@Repository
public interface FactoryJpaAdapter extends FactoryRepository, org.springframework.data.jpa.repository.JpaRepository<Factory, Long> {

    Optional<Factory> findByIdAndDeletedIsFalse(Long id);

    Optional<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName);

    Page<Factory> findByCooperationStatusAndDeletedIsFalse(CooperationStatus cooperationStatus, Pageable pageable);

    Page<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName, Pageable pageable);

    @Override
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Factory f WHERE f.deleted = false")
    boolean existsByDeletedIsFalse();

    @Override
    long countByCooperationStatusAndDeletedIsFalse(CooperationStatus status);

    @Override
    long countByDeletedIsFalse();

    @Override
    default Map<Long, Factory> findAllByIdInAndDeletedIsFalse(List<Long> ids) {
        return findAllById(ids).stream()
                .filter(f -> !f.isDeleted())
                .collect(Collectors.toMap(Factory::getId, f -> f));
    }
}
