package com.manpou.allinone.procurement.domain.repository;

import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 发注单仓库接口（领域层契约，不感知 JPA）。
 */
public interface ProcurementRepository {

    Optional<Procurement> findById(Long id);

    Optional<Procurement> findByIdAndDeletedIsFalse(Long id);

    Procurement save(Procurement entity);

    void deleteById(Long id);

    List<Procurement> findAllByDeletedIsFalse();

    Page<Procurement> findAllByDeletedIsFalse(Pageable pageable);

    Page<Procurement> findByStatusAndDeletedIsFalse(ShipmentStatus status, Pageable pageable);

    Page<Procurement> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);

    Page<Procurement> findByCustomerCompanyAndDeletedIsFalse(String customerCompany, Pageable pageable);

    Page<Procurement> findByFactoryIdAndDeletedIsFalse(Long factoryId, Pageable pageable);

    boolean existsActiveByFactoryId(Long factoryId);

    /** 按 ID 批量查询（v1.6.0：撤销转换时批量加载状态检查） */
    List<Procurement> findAllByIdInAndDeletedIsFalse(List<Long> ids);
}
