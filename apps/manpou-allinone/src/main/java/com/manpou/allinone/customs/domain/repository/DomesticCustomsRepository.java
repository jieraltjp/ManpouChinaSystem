package com.manpou.allinone.customs.domain.repository;

import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 国内报关记录仓库接口（领域层契约）。
 * 注意：@Repository 标记使 Spring Data JPA 注册为 Bean。
 */
@Repository
public interface DomesticCustomsRepository extends JpaRepository<DomesticCustomsRecord, Long> {

    Optional<DomesticCustomsRecord> findByIdAndDeletedIsFalse(Long id);

    Optional<DomesticCustomsRecord> findByLogisticsPlanIdAndDeletedIsFalse(Long logisticsPlanId);

    List<DomesticCustomsRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<DomesticCustomsRecord> findByContainerNoContainingAndDeletedIsFalse(String containerNo, Pageable pageable);

    Page<DomesticCustomsRecord> findAllByDeletedIsFalse(Pageable pageable);
}
