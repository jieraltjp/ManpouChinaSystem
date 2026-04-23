package com.manpou.allinone.common.port;

/**
 * 跨模块：采购单查询接口。
 * 避免 factory 模块直接依赖 procurement 模块的 repository。
 */
public interface ProcurementPort {

    /**
     * 检查工厂是否有未终态发注单。
     */
    boolean existsActiveByFactoryId(Long factoryId);
}
