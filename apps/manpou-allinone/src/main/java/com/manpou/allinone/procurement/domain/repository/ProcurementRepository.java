package com.manpou.allinone.procurement.domain.repository;

import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 发注单仓库接口。
 */
@Repository
public interface ProcurementRepository extends JpaRepository<Procurement, Long> {

    Optional<Procurement> findByProductCodeAndIsDeletedFalse(String productCode);

    Optional<Procurement> findByIdAndIsDeletedFalse(Long id);

    List<Procurement> findAllByIsDeletedFalse();

    Page<Procurement> findAllByIsDeletedFalse(Pageable pageable);

    Page<Procurement> findByStatusAndIsDeletedFalse(ShipmentStatus status, Pageable pageable);

    Page<Procurement> findByProductCodeAndIsDeletedFalse(String productCode, Pageable pageable);

    Page<Procurement> findByCustomerCompanyAndIsDeletedFalse(String customerCompany, Pageable pageable);
}
