package com.manpou.allinone.sales.domain.repository;

import com.manpou.allinone.sales.domain.model.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    Optional<SalesRecord> findByIdAndIsDeletedFalse(Long id);

    List<SalesRecord> findByProductCodeAndIsDeletedFalse(String productCode);

    List<SalesRecord> findByProcurementIdAndIsDeletedFalse(Long procurementId);

    List<SalesRecord> findByIsDeletedFalseOrderByCreateTimeDesc();

    List<SalesRecord> findByStatusInAndIsDeletedFalseOrderByCreateTimeDesc(List<String> statuses);
}
