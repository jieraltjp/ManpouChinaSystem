package com.manpou.allinone.customs.infrastructure.port;

import com.manpou.allinone.common.port.CustomsQueryPort;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import com.manpou.allinone.customs.domain.repository.JapanCustomsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomsQueryPortImpl implements CustomsQueryPort {

    private final DomesticCustomsRepository domesticCustomsRepository;
    private final JapanCustomsRepository japanCustomsRepository;

    @Override
    public Optional<DomesticCustomsRecord> findDomesticById(Long id) {
        return domesticCustomsRepository.findByIdAndDeletedIsFalse(id);
    }

    @Override
    public Optional<JapanCustomsRecord> findJapanById(Long id) {
        return japanCustomsRepository.findByIdAndDeletedIsFalse(id);
    }
}
