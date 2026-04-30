package com.manpou.allinone.replenishment.infrastructure.persistence.jpa;

import com.manpou.allinone.replenishment.domain.model.DemandProcurementMapping;
import com.manpou.allinone.replenishment.domain.model.MappingStatus;
import com.manpou.allinone.replenishment.domain.repository.DemandProcurementMappingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 需求-采购分配映射 JPA 持久化适配器（基础设施层）。
 */
@Repository
public interface JpaDemandProcurementMappingRepository
        extends DemandProcurementMappingRepository, JpaRepository<DemandProcurementMapping, Long> {

    List<DemandProcurementMapping> findByDemandIdAndDeletedIsFalse(Long demandId);

    List<DemandProcurementMapping> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<DemandProcurementMapping> findByDemandIdAndDeletedIsFalse(Long demandId, Pageable pageable);

    Page<DemandProcurementMapping> findByStatusAndDeletedIsFalse(MappingStatus status, Pageable pageable);

    boolean existsByDemandIdAndProcurementIdAndDeletedIsFalse(Long demandId, Long procurementId);

    @Override
    Optional<DemandProcurementMapping> findById(Long id);

    @Override
    Optional<DemandProcurementMapping> findByIdAndDeletedIsFalse(Long id);
}
