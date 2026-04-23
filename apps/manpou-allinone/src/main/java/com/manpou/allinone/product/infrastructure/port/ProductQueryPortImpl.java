package com.manpou.allinone.product.infrastructure.port;

import com.manpou.allinone.common.port.ProductQueryPort;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductQueryPortImpl implements ProductQueryPort {

    private final ProductRepository productRepository;

    public ProductQueryPortImpl(
            @Qualifier("productJpaRepository") ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findByMasterCode(String masterCode) {
        return productRepository.findByMasterCodeAndDeletedIsFalse(masterCode);
    }

    @Override
    public List<Product> findByMasterCodeAndDeletedIsFalse(String masterCode) {
        return productRepository.findAllByMasterCodeAndDeletedIsFalse(masterCode);
    }
}

