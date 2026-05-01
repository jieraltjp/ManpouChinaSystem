package com.manpou.allinone.qc.domain.repository;

import com.manpou.allinone.qc.domain.model.QcImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QcImageRepository extends JpaRepository<QcImage, Long> {

    List<QcImage> findByQcRecordIdAndIsDeletedFalse(Long qcRecordId);

    Optional<QcImage> findByIdAndIsDeletedFalse(Long id);

    long countByQcRecordIdAndIsDeletedFalse(Long qcRecordId);
}
