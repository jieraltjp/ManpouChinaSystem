package com.manpou.allinone.factory.infrastructure.port;

import com.manpou.allinone.common.port.FactorySynergyPort;
import com.manpou.allinone.product.domain.repository.ProductFactoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工厂协同状态查询口实现。
 * 通过 product_factory 表检查工厂是否有关联商品。
 *
 * <p>ProductFactoryRepository 不扩展 JpaRepository，仅 ProductFactoryJpaRepository 扩展之，
 * 故 Spring 只注册一个 Bean，无 @Qualifier 必要。
 *
 * @see FactorySynergyPort#hasAssociatedProducts
 */
@Component
public class FactorySynergyPortImpl implements FactorySynergyPort {

    private final ProductFactoryRepository productFactoryRepository;

    public FactorySynergyPortImpl(ProductFactoryRepository productFactoryRepository) {
        this.productFactoryRepository = productFactoryRepository;
    }

    @Override
    public boolean hasAssociatedProducts(Long factoryId) {
        List<?> associations = productFactoryRepository.findByFactoryId(factoryId);
        return !associations.isEmpty();
    }
}
