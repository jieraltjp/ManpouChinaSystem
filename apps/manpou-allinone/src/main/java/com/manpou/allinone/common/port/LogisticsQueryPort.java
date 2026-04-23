package com.manpou.allinone.common.port;

import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import java.util.List;
import java.util.Optional;

/**
 * 跨模块：物流计划查询接口。
 * 避免 order / customs 等模块直接依赖 logistics 模块。
 */
public interface LogisticsQueryPort {

    Optional<LogisticsPlan> findById(Long id);

    List<LogisticsPlan> findByProcurementId(Long procurementId);
}
