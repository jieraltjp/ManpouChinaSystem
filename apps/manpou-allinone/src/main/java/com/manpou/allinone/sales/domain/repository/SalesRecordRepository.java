package com.manpou.allinone.sales.domain.repository;

import com.manpou.allinone.sales.domain.model.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    Optional<SalesRecord> findByIdAndDeletedIsFalse(Long id);

    List<SalesRecord> findByProductCodeAndDeletedIsFalse(String productCode);

    List<SalesRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    List<SalesRecord> findByDeletedFalseOrderByCreateTimeDesc();

    List<SalesRecord> findByStatusInAndDeletedFalseOrderByCreateTimeDesc(List<String> statuses);
}
