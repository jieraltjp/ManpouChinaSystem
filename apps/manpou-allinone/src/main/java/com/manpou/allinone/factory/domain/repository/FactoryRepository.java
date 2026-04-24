package com.manpou.allinone.factory.domain.repository;

import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 工厂仓库接口（领域层契约，不感知 JPA）。
 */
public interface FactoryRepository {

    Optional<Factory> findById(Long id);

    Optional<Factory> findByIdAndDeletedIsFalse(Long id);

    Optional<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName);

    Factory save(Factory entity);

    void deleteById(Long id);

    List<Factory> findAllByDeletedIsFalse();

    Page<Factory> findAllByDeletedIsFalse(Pageable pageable);

    Page<Factory> findByCooperationStatusAndDeletedIsFalse(CooperationStatus cooperationStatus, Pageable pageable);

    Page<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName, Pageable pageable);

    boolean existsByDeletedIsFalse();

    long countByCooperationStatusAndDeletedIsFalse(CooperationStatus status);

    long countByDeletedIsFalse();
}
