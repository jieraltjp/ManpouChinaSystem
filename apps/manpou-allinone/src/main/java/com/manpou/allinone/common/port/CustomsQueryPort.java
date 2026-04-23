package com.manpou.allinone.common.port;

import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import java.util.Optional;

/**
 * 跨模块：报关记录查询接口。
 * 避免 order / logistics 等模块直接依赖 customs 模块。
 */
public interface CustomsQueryPort {

    Optional<DomesticCustomsRecord> findDomesticById(Long id);

    Optional<JapanCustomsRecord> findJapanById(Long id);
}
