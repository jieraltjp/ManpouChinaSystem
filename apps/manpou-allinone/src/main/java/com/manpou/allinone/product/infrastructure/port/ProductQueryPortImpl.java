package com.manpou.allinone.product.infrastructure.port;

import com.manpou.allinone.common.port.ProductQueryPort;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductQueryPortImpl implements ProductQueryPort {

    private final ProductRepository productRepository;
    private final ProductJpaRepository productJpaRepository;

    public ProductQueryPortImpl(
            @Qualifier("productJpaRepository") ProductRepository productRepository,
            ProductJpaRepository productJpaRepository) {
        this.productRepository = productRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Optional<Product> findByMasterCode(String masterCode) {
        return productRepository.findByMasterCodeAndDeletedIsFalse(masterCode);
    }

    @Override
    public List<Product> findByMasterCodeAndDeletedIsFalse(String masterCode) {
        return productRepository.findAllByMasterCodeAndDeletedIsFalse(masterCode);
    }

    @Override
    public List<Product> findByMasterCodeIn(List<String> masterCodes) {
        return productRepository.findAllByMasterCodeInAndDeletedIsFalse(masterCodes);
    }

    @Override
    public List<Object[]> findMasterCodeSuggestions(String keyword) {
        return productJpaRepository.findMasterCodeSuggestions(keyword);
    }

    @Override
    public List<Object[]> findSubCodesByMasterCode(String masterCode) {
        return productJpaRepository.findSubCodesByMasterCode(masterCode);
    }
}

