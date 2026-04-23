package com.manpou.allinone.qc.infrastructure.persistence.jpa;

import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 * 同时满足领域接口契约和 JPA 能力。
 */
@Repository
public interface QcRecordJpaRepository extends QcRecordRepository, JpaRepository<QcRecord, Long> {

    Optional<QcRecord> findByQcCodeAndDeletedIsFalse(String qcCode);

    Page<QcRecord> findByResultAndDeletedIsFalse(QcResult result, Pageable pageable);

    Page<QcRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    Page<QcRecord> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);
}
