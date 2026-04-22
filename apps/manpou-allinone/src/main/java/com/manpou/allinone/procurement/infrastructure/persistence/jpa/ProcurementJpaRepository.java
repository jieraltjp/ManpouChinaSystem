package com.manpou.allinone.procurement.infrastructure.persistence.jpa;

import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 */
@Repository
public interface ProcurementJpaRepository extends ProcurementRepository, JpaRepository<Procurement, Long> {

    Page<Procurement> findByStatusAndIsDeletedFalse(ShipmentStatus status, Pageable pageable);

    Page<Procurement> findByProductCodeAndIsDeletedFalse(String productCode, Pageable pageable);

    Page<Procurement> findByCustomerCompanyAndIsDeletedFalse(String customerCompany, Pageable pageable);

    Page<Procurement> findByFactoryIdAndIsDeletedFalse(Long factoryId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Procurement p " +
           "WHERE p.factoryId = :factoryId " +
           "  AND p.isDeleted = false " +
           "  AND p.status NOT IN ('完了', '退货')")
    boolean existsActiveByFactoryId(@Param("factoryId") Long factoryId);
}
