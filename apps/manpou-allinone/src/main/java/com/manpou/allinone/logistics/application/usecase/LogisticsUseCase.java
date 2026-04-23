package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.logistics.application.dto.LogisticsCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsUpdateCmd;
import com.manpou.allinone.logistics.application.assembler.LogisticsAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.logistics.domain.model.LogisticsExample;
import com.manpou.allinone.logistics.domain.repository.LogisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 示例用例服务。
 * 负责编排业务操作，不含领域逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsUseCase {

    private final LogisticsRepository logisticsRepository;
    private final LogisticsAssembler logisticsAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<LogisticsPageQuery> pageQuery(LogisticsQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<LogisticsExample> page = logisticsRepository.findAllByDeletedIsFalse(pageRequest);
        return page.map(logisticsAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public LogisticsPageQuery getById(Long id) {
        LogisticsExample entity = logisticsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("LogisticsExample", id));
        return logisticsAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(LogisticsCreateCmd cmd) {
        LogisticsExample entity = logisticsAssembler.toEntity(cmd);
        LogisticsExample saved = logisticsRepository.save(entity);
        log.info("[LogisticsExample] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, LogisticsUpdateCmd cmd) {
        LogisticsExample entity = logisticsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("LogisticsExample", id));
        logisticsAssembler.copyToEntity(cmd, entity);
        logisticsRepository.save(entity);
        log.info("[LogisticsExample] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        LogisticsExample entity = logisticsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("LogisticsExample", id));
        entity.markDeleted();
        logisticsRepository.save(entity);
        log.info("[LogisticsExample] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
