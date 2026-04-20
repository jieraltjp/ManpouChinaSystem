package com.manpou.allinone.finance.application.usecase;

import com.manpou.allinone.finance.application.dto.FinanceCreateCmd;
import com.manpou.allinone.finance.application.dto.FinancePageQuery;
import com.manpou.allinone.finance.application.dto.FinanceQuery;
import com.manpou.allinone.finance.application.dto.FinanceUpdateCmd;
import com.manpou.allinone.finance.application.assembler.FinanceAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.finance.domain.model.FinanceExample;
import com.manpou.allinone.finance.domain.repository.FinanceRepository;
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
public class FinanceUseCase {

    private final FinanceRepository exampleRepository;
    private final FinanceAssembler exampleAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<FinancePageQuery> pageQuery(FinanceQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<FinanceExample> page = exampleRepository.findAllByIsDeletedFalse(pageRequest);
        return page.map(exampleAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public FinancePageQuery getById(Long id) {
        FinanceExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("FinanceExample", id));
        return exampleAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(FinanceCreateCmd cmd) {
        FinanceExample entity = exampleAssembler.toEntity(cmd);
        FinanceExample saved = exampleRepository.save(entity);
        log.info("[FinanceExample] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, FinanceUpdateCmd cmd) {
        FinanceExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("FinanceExample", id));
        exampleAssembler.copyToEntity(cmd, entity);
        exampleRepository.save(entity);
        log.info("[FinanceExample] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        FinanceExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("FinanceExample", id));
        entity.markDeleted();
        exampleRepository.save(entity);
        log.info("[FinanceExample] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
