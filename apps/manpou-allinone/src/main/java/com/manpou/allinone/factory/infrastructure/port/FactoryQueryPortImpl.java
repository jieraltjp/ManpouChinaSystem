package com.manpou.allinone.factory.infrastructure.port;

import com.manpou.allinone.common.port.FactoryQueryPort;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductFactoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FactoryQueryPortImpl implements FactoryQueryPort {

    private final FactoryRepository factoryRepository;
    private final ProductFactoryJpaRepository productFactoryJpaRepository;

    @Override
    public Optional<Factory> findById(Long id) {
        return factoryRepository.findById(id);
    }

    @Override
    public Optional<Factory> findByIdAndDeletedIsFalse(Long id) {
        return factoryRepository.findByIdAndDeletedIsFalse(id);
    }

    @Override
    public long countFactoriesWithLinkedProducts() {
        return productFactoryJpaRepository.countFactoriesWithLinkedProducts();
    }
}
