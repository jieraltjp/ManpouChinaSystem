package com.manpou.allinone.customs.domain.repository;

import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JapanCustomsRepository extends JpaRepository<JapanCustomsRecord, Long> {

    Optional<JapanCustomsRecord> findByIdAndDeletedIsFalse(Long id);

    List<JapanCustomsRecord> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    Optional<JapanCustomsRecord> findByDomesticCustomsIdAndDeletedIsFalse(Long domesticCustomsId);

    List<JapanCustomsRecord> findByDeletedFalseOrderByCreateTimeDesc();
}
