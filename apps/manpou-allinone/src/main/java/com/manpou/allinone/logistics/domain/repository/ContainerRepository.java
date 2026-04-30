package com.manpou.allinone.logistics.domain.repository;

import com.manpou.allinone.logistics.domain.model.Container;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 货柜仓库接口（领域层契约，不感知 JPA）。
 */
public interface ContainerRepository {

    Optional<Container> findById(Long id);

    Optional<Container> findByIdAndDeletedIsFalse(Long id);

    Optional<Container> findByContainerNoAndDeletedIsFalse(String containerNo);

    Container save(Container entity);

    void deleteById(Long id);

    Page<Container> findAllByDeletedIsFalse(Pageable pageable);

    Page<Container> findByStatusAndDeletedIsFalse(ContainerStatus status, Pageable pageable);

    Page<Container> findByPoolIdAndDeletedIsFalse(Long poolId, Pageable pageable);
}
