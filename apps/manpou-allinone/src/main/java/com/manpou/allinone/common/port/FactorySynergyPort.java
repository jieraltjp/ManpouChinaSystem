package com.manpou.allinone.common.port;

/**
 * 跨模块：工厂协同状态查询口。
 * 检查工厂是否与商品存在关联（product_factory 表）。
 * 避免 factory 模块直接依赖 product 模块的 entity/repository。
 *
 * <p>渲染规则：当 product_factory 表中存在 factory_id 关联记录时，
 * Factory API 返回的 cooperationStatus 应强制渲染为 ACTIVE（合作中）。
 *
 * @see com.manpou.allinone.factory.application.assembler.FactoryAssembler#toDto
 */
public interface FactorySynergyPort {

    /**
     * 工厂是否有关联商品。
     *
     * @param factoryId 工厂 ID
     * @return true = 在 product_factory 表中存在 factory_id 关联记录
     */
    boolean hasAssociatedProducts(Long factoryId);
}
