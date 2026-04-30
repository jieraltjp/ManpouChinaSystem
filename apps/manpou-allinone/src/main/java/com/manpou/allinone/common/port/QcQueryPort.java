package com.manpou.allinone.common.port;

import com.manpou.allinone.qc.domain.model.QcRecord;
import java.util.Optional;

/**
 * 跨模块：质检记录查询接口。
 * 避免 procurement / order 等模块直接依赖 qc 模块。
 */
public interface QcQueryPort {

    Optional<QcRecord> findById(Long id);

    /** 按采购单聚合合格数量（SPEC-B00 数量等式）。 */
    Integer sumPassedCountByProcurementId(Long procurementId);
}
