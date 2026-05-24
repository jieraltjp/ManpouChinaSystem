package com.manpou.allinone.replenishment.domain.repository;

import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplenishmentDemandRepository extends JpaRepository<ReplenishmentDemand, Long>, JpaSpecificationExecutor<ReplenishmentDemand> {

    Optional<ReplenishmentDemand> findByIdAndDeletedIsFalse(Long id);

    List<ReplenishmentDemand> findAllByDeletedIsFalse();

    Page<ReplenishmentDemand> findAllByDeletedIsFalse(Pageable pageable);

    Page<ReplenishmentDemand> findByStatusAndDeletedIsFalse(DemandStatus status, Pageable pageable);

    Page<ReplenishmentDemand> findByDemandTypeAndDeletedIsFalse(DemandType type, Pageable pageable);


    Page<ReplenishmentDemand> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);

    /**
     * 货号模糊搜索：同时匹配主货号和子货号（OR）。
     * 查询条件：deleted=false AND (productCode LIKE %keyword% OR subProductCode LIKE %keyword%)
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT d FROM ReplenishmentDemand d WHERE d.deleted = false " +
        "AND (LOWER(d.productCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR  LOWER(d.subProductCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ReplenishmentDemand> findByProductCodeOrSubProductCodeFuzzy(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);

    List<ReplenishmentDemand> findAllByIdInAndDeletedIsFalse(List<Long> ids);

    @Query("SELECT DISTINCT d.destination FROM ReplenishmentDemand d WHERE d.deleted = false AND d.destination IS NOT NULL AND d.destination <> '' ORDER BY d.destination")
    List<String> findDistinctDestinations();

    @Query("SELECT DISTINCT d.japanLead FROM ReplenishmentDemand d WHERE d.deleted = false AND d.japanLead IS NOT NULL AND d.japanLead <> '' ORDER BY d.japanLead")
    List<String> findDistinctJapanLeads();

    /** 按关联发注单 ID 查询（v2.0.0：Procurement → ReplenishmentDemand 反向关联） */
    List<ReplenishmentDemand> findByLinkedProcurementIdAndDeletedIsFalse(Long linkedProcurementId);
}
