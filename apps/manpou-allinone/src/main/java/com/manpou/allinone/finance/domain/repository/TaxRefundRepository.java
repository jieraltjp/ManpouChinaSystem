package com.manpou.allinone.finance.domain.repository;

import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRefundRepository extends JpaRepository<TaxRefundRecord, Long> {

    Optional<TaxRefundRecord> findByIdAndIsDeletedFalse(Long id);

    List<TaxRefundRecord> findByProcurementIdAndIsDeletedFalse(Long procurementId);

    Optional<TaxRefundRecord> findByJapanCustomsIdAndIsDeletedFalse(Long japanCustomsId);

    List<TaxRefundRecord> findByIsDeletedFalseOrderByCreateTimeDesc();
}
