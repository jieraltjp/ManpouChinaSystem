package com.manpou.allinone.qc.domain.repository;

import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 验货记录仓库接口（领域层契约，不感知 JPA）。
 */
public interface QcRecordRepository {

    Optional<QcRecord> findById(Long id);

    Optional<QcRecord> findByIdAndDeletedIsFalse(Long id);

    Optional<QcRecord> findByQcCodeAndDeletedIsFalse(String qcCode);

    QcRecord save(QcRecord entity);

    void flush();

    void deleteById(Long id);

    Page<QcRecord> findAllByDeletedIsFalse(Pageable pageable);

    Page<QcRecord> findByResultAndDeletedIsFalse(QcResult result, Pageable pageable);

    Page<QcRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    List<QcRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Page<QcRecord> findByProductCodeAndDeletedIsFalse(String productCode, Pageable pageable);

    Page<QcRecord> findByShipmentBatchIdAndDeletedIsFalse(Long shipmentBatchId, Pageable pageable);

    List<QcRecord> findByShipmentBatchIdAndDeletedIsFalse(Long shipmentBatchId);

    /** 按采购单聚合合格数量。 */
    Integer sumPassedCountByProcurementId(Long procurementId);
}
