package com.manpou.allinone.factory.domain.repository;

import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 工厂仓库接口（领域层契约，不感知 JPA）。
 */
public interface FactoryRepository {

    Optional<Factory> findById(Long id);

    Optional<Factory> findByIdAndIsDeletedFalse(Long id);

    Optional<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName);

    Factory save(Factory entity);

    void deleteById(Long id);

    List<Factory> findAllByIsDeletedFalse();

    Page<Factory> findAllByIsDeletedFalse(Pageable pageable);

    Page<Factory> findByStatusAndIsDeletedFalse(FactoryStatus status, Pageable pageable);

    Page<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName, Pageable pageable);

    boolean existsByIsDeletedFalse();
}
