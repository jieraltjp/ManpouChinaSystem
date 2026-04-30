package com.manpou.allinone.logistics.infrastructure.persistence.jpa;

import com.manpou.allinone.logistics.domain.model.Container;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 货柜 JPA 持久化适配器。
 */
@Repository
public interface JpaContainerRepository extends ContainerRepository, JpaRepository<Container, Long> {

    Optional<Container> findByIdAndDeletedIsFalse(Long id);

    Optional<Container> findByContainerNoAndDeletedIsFalse(String containerNo);

    Page<Container> findByStatusAndDeletedIsFalse(ContainerStatus status, Pageable pageable);

    Page<Container> findByPoolIdAndDeletedIsFalse(Long poolId, Pageable pageable);
}
