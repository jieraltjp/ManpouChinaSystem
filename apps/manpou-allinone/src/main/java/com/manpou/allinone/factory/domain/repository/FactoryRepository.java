package com.manpou.allinone.factory.domain.repository;

import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactoryRepository extends JpaRepository<Factory, Long> {

    Optional<Factory> findByIdAndIsDeletedFalse(Long id);

    Optional<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName);

    Page<Factory> findByFactoryNameAndIsDeletedFalse(String factoryName, Pageable pageable);

    List<Factory> findAllByIsDeletedFalse();

    Page<Factory> findAllByIsDeletedFalse(Pageable pageable);

    Page<Factory> findByStatusAndIsDeletedFalse(FactoryStatus status, Pageable pageable);
}
