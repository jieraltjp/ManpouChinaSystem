package com.manpou.allinone.logistics.infrastructure.persistence.jpa;

import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 */
@Repository
public interface LogisticsPlanJpaRepository extends LogisticsPlanRepository, JpaRepository<LogisticsPlan, Long> {

    Page<LogisticsPlan> findByStatusAndDeletedIsFalse(LogisticsStatus status, Pageable pageable);

    Page<LogisticsPlan> findByPlanTypeAndDeletedIsFalse(PlanType planType, Pageable pageable);

    Page<LogisticsPlan> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    Page<LogisticsPlan> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);
}
