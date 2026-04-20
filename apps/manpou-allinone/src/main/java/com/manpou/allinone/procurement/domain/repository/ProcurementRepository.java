package com.manpou.allinone.procurement.domain.repository;

import com.manpou.allinone.procurement.domain.model.ProcurementExample;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 发注单仓库接口。
 * 定义领域层需要的查询方法。
 * TODO Phase A: 根据真实 ShippingOrder 实体扩展查询方法。
 */
@Repository
public interface ProcurementRepository extends JpaRepository<ProcurementExample, Long> {

    Optional<ProcurementExample> findByNameAndIsDeletedFalse(String name);

    Optional<ProcurementExample> findByIdAndIsDeletedFalse(Long id);

    List<ProcurementExample> findAllByIsDeletedFalse();

    Page<ProcurementExample> findAllByIsDeletedFalse(Pageable pageable);

    Page<ProcurementExample> findByStatusAndIsDeletedFalse(ShipmentStatus status, Pageable pageable);
}
