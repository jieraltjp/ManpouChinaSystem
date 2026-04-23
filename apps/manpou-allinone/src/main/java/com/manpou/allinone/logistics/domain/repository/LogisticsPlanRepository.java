package com.manpou.allinone.logistics.domain.repository;

import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 调配计划仓库接口（领域层契约，不感知 JPA）。
 */
public interface LogisticsPlanRepository {

    Optional<LogisticsPlan> findById(Long id);

    Optional<LogisticsPlan> findByIdAndDeletedIsFalse(Long id);

    LogisticsPlan save(LogisticsPlan entity);

    void deleteById(Long id);

    Page<LogisticsPlan> findAllByDeletedIsFalse(Pageable pageable);

    Page<LogisticsPlan> findByStatusAndDeletedIsFalse(LogisticsStatus status, Pageable pageable);

    Page<LogisticsPlan> findByPlanTypeAndDeletedIsFalse(PlanType planType, Pageable pageable);

    Page<LogisticsPlan> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    List<LogisticsPlan> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<LogisticsPlan> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);
}
