package com.manpou.allinone.infrastructure.port;

import com.manpou.allinone.common.port.ProcurementPort;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ProcurementPort 的实现，依赖于 procurement 模块的 repository。
 * 放在 infrastructure 层，避免 factory 模块直接依赖 procurement。
 */
@Component
@RequiredArgsConstructor
public class ProcurementPortImpl implements ProcurementPort {

    private final ProcurementRepository procurementRepository;

    @Override
    public boolean existsActiveByFactoryId(Long factoryId) {
        return procurementRepository.existsActiveByFactoryId(factoryId);
    }

    @Override
    public Optional<Long> findFactoryIdById(Long id) {
        return procurementRepository.findByIdAndDeletedIsFalse(id)
                .map(procurement -> procurement.getFactoryId());
    }
}
