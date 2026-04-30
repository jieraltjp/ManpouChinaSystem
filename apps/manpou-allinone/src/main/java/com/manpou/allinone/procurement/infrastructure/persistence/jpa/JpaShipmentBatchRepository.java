package com.manpou.allinone.procurement.infrastructure.persistence.jpa;

import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import com.manpou.allinone.procurement.domain.repository.ShipmentBatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 出货批次 JPA 持久化适配器（基础设施层）。
 */
@Repository
public interface JpaShipmentBatchRepository extends ShipmentBatchRepository, JpaRepository<ShipmentBatch, Long> {

    List<ShipmentBatch> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<ShipmentBatch> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    long countByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<ShipmentBatch> findByStatusAndDeletedIsFalse(ShipmentBatchStatus status, Pageable pageable);

    List<ShipmentBatch> findByProcurementIdInAndDeletedIsFalse(List<Long> procurementIds);

    @Override
    Optional<ShipmentBatch> findById(Long id);

    @Override
    Optional<ShipmentBatch> findByIdAndDeletedIsFalse(Long id);
}
