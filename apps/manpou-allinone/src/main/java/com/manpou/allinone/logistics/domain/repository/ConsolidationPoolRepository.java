package com.manpou.allinone.logistics.domain.repository;

import com.manpou.allinone.logistics.domain.model.ConsolidationPool;
import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 拼柜池仓库接口（领域层契约，不感知 JPA）。
 */
public interface ConsolidationPoolRepository {

    Optional<ConsolidationPool> findById(Long id);

    Optional<ConsolidationPool> findByIdAndDeletedIsFalse(Long id);

    Optional<ConsolidationPool> findByPoolCodeAndDeletedIsFalse(String poolCode);

    ConsolidationPool save(ConsolidationPool entity);

    void deleteById(Long id);

    Page<ConsolidationPool> findAllByDeletedIsFalse(Pageable pageable);

    Page<ConsolidationPool> findByStatusAndDeletedIsFalse(ConsolidationPoolStatus status, Pageable pageable);

    Page<ConsolidationPool> findByDestinationPortAndDeletedIsFalse(String destinationPort, Pageable pageable);
}
