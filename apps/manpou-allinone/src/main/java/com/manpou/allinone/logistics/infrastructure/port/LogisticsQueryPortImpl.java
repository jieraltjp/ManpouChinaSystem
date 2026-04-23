package com.manpou.allinone.logistics.infrastructure.port;

import com.manpou.allinone.common.port.LogisticsQueryPort;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LogisticsQueryPortImpl implements LogisticsQueryPort {

    private final LogisticsPlanRepository logisticsPlanRepository;

    @Override
    public Optional<LogisticsPlan> findById(Long id) {
        return logisticsPlanRepository.findByIdAndDeletedIsFalse(id);
    }

    @Override
    public List<LogisticsPlan> findByProcurementId(Long procurementId) {
        return logisticsPlanRepository.findByProcurementIdAndDeletedIsFalse(procurementId);
    }
}
