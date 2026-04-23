package com.manpou.allinone.finance.domain.repository;

import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRefundRepository extends JpaRepository<TaxRefundRecord, Long> {

    Optional<TaxRefundRecord> findByIdAndDeletedIsFalse(Long id);

    List<TaxRefundRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Optional<TaxRefundRecord> findByJapanCustomsIdAndDeletedIsFalse(Long japanCustomsId);

    List<TaxRefundRecord> findByDeletedFalseOrderByCreateTimeDesc();
}
