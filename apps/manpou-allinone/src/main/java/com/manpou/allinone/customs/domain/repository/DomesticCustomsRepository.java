package com.manpou.allinone.customs.domain.repository;

import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomesticCustomsRepository extends JpaRepository<DomesticCustomsRecord, Long> {

    Optional<DomesticCustomsRecord> findByIdAndIsDeletedFalse(Long id);

    List<DomesticCustomsRecord> findByProcurementIdAndIsDeletedFalse(Long procurementId);

    Optional<DomesticCustomsRecord> findByLogisticsPlanIdAndIsDeletedFalse(Long logisticsPlanId);

    List<DomesticCustomsRecord> findByIsDeletedFalseOrderByCreateTimeDesc();
}
