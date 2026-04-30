package com.manpou.allinone.qc.infrastructure.port;

import com.manpou.allinone.common.port.QcQueryPort;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QcQueryPortImpl implements QcQueryPort {

    private final QcRecordRepository qcRecordRepository;

    @Override
    public Optional<QcRecord> findById(Long id) {
        return qcRecordRepository.findByIdAndDeletedIsFalse(id);
    }

    @Override
    public Integer sumPassedCountByProcurementId(Long procurementId) {
        return qcRecordRepository.sumPassedCountByProcurementId(procurementId);
    }
}
