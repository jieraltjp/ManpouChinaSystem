package com.manpou.allinone.logistics.domain.repository;

import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogisticsPlanRepository extends JpaRepository<LogisticsPlan, Long> {

    Optional<LogisticsPlan> findByIdAndIsDeletedFalse(Long id);

    Page<LogisticsPlan> findAllByIsDeletedFalse(Pageable pageable);

    Page<LogisticsPlan> findByStatusAndIsDeletedFalse(LogisticsStatus status, Pageable pageable);

    Page<LogisticsPlan> findByPlanTypeAndIsDeletedFalse(PlanType planType, Pageable pageable);

    Page<LogisticsPlan> findByProcurementIdAndIsDeletedFalse(Long procurementId, Pageable pageable);

    Page<LogisticsPlan> findByProductCodeAndIsDeletedFalse(String productCode, Pageable pageable);
}
