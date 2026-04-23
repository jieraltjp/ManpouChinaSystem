package com.manpou.allinone.customs.domain.repository;

import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JapanCustomsRepository extends JpaRepository<JapanCustomsRecord, Long> {

    Optional<JapanCustomsRecord> findByIdAndIsDeletedFalse(Long id);

    List<JapanCustomsRecord> findByProcurementIdAndIsDeletedFalse(Long procurementId);

    Optional<JapanCustomsRecord> findByDomesticCustomsIdAndIsDeletedFalse(Long domesticCustomsId);

    List<JapanCustomsRecord> findByIsDeletedFalseOrderByCreateTimeDesc();
}
