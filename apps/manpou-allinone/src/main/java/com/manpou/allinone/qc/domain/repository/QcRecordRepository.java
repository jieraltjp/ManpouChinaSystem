package com.manpou.allinone.qc.domain.repository;

import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QcRecordRepository extends JpaRepository<QcRecord, Long> {

    Optional<QcRecord> findByIdAndIsDeletedFalse(Long id);

    Optional<QcRecord> findByQcCodeAndIsDeletedFalse(String qcCode);

    Page<QcRecord> findAllByIsDeletedFalse(Pageable pageable);

    Page<QcRecord> findByResultAndIsDeletedFalse(QcResult result, Pageable pageable);

    Page<QcRecord> findByProcurementIdAndIsDeletedFalse(Long procurementId, Pageable pageable);

    Page<QcRecord> findByProductCodeAndIsDeletedFalse(String productCode, Pageable pageable);
}
