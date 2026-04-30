package com.manpou.allinone.logistics.infrastructure.persistence.jpa;

import com.manpou.allinone.logistics.domain.model.ConsolidationPool;
import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import com.manpou.allinone.logistics.domain.repository.ConsolidationPoolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 拼柜池 JPA 持久化适配器。
 */
@Repository
public interface JpaConsolidationPoolRepository extends ConsolidationPoolRepository, JpaRepository<ConsolidationPool, Long> {

    Optional<ConsolidationPool> findByIdAndDeletedIsFalse(Long id);

    Optional<ConsolidationPool> findByPoolCodeAndDeletedIsFalse(String poolCode);

    Page<ConsolidationPool> findByStatusAndDeletedIsFalse(ConsolidationPoolStatus status, Pageable pageable);

    Page<ConsolidationPool> findByDestinationPortAndDeletedIsFalse(String destinationPort, Pageable pageable);
}
