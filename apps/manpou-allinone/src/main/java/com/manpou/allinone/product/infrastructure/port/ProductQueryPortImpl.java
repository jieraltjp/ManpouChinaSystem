package com.manpou.allinone.product.infrastructure.port;

import com.manpou.allinone.common.port.ProductQueryPort;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductQueryPortImpl implements ProductQueryPort {

    private final ProductRepository productRepository;

    @Override
    public Optional<Product> findByMasterCode(String masterCode) {
        return productRepository.findByMasterCodeAndDeletedIsFalse(masterCode);
    }

    @Override
    public List<Product> findByMasterCodeAndDeletedIsFalse(String masterCode) {
        return productRepository.findAllByMasterCodeAndDeletedIsFalse(masterCode);
    }
}

