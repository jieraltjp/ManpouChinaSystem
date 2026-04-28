package com.manpou.allinone.order.domain.repository;

import com.manpou.allinone.order.domain.model.ProcurementSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 发注单快照仓库。
 */
@Repository
public interface ProcurementSnapshotRepository extends JpaRepository<ProcurementSnapshot, Long> {

    Optional<ProcurementSnapshot> findByProcurementId(Long procurementId);
}
