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

    Optional<ReplenishmentDemand> findByIdAndDeletedIsFalse(Long id);

    List<ReplenishmentDemand> findAllByDeletedIsFalse();

    Page<ReplenishmentDemand> findAllByDeletedIsFalse(Pageable pageable);

    Page<ReplenishmentDemand> findByStatusAndDeletedIsFalse(DemandStatus status, Pageable pageable);

    Page<ReplenishmentDemand> findByDemandTypeAndDeletedIsFalse(DemandType type, Pageable pageable);

    Optional<ReplenishmentDemand> findByLinkedProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<ReplenishmentDemand> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);
}
