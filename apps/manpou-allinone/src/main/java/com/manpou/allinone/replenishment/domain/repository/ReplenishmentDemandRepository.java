package com.manpou.allinone.replenishment.domain.repository;

import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplenishmentDemandRepository extends JpaRepository<ReplenishmentDemand, Long> {

    Optional<ReplenishmentDemand> findByIdAndIsDeletedFalse(Long id);

    List<ReplenishmentDemand> findAllByIsDeletedFalse();

    Page<ReplenishmentDemand> findAllByIsDeletedFalse(Pageable pageable);

    Page<ReplenishmentDemand> findByStatusAndIsDeletedFalse(DemandStatus status, Pageable pageable);

    Page<ReplenishmentDemand> findByDemandTypeAndIsDeletedFalse(DemandType type, Pageable pageable);

    Optional<ReplenishmentDemand> findByLinkedProcurementIdAndIsDeletedFalse(Long procurementId);

    Page<ReplenishmentDemand> findByProductCodeAndIsDeletedFalse(String productCode, Pageable pageable);
}
