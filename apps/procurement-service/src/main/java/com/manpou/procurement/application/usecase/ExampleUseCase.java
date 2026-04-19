package com.manpou.procurement.application.usecase;

import com.manpou.procurement.application.dto.ExampleCreateCmd;
import com.manpou.procurement.application.dto.ExamplePageQuery;
import com.manpou.procurement.application.dto.ExampleQuery;
import com.manpou.procurement.application.dto.ExampleUpdateCmd;
import com.manpou.procurement.application.assembler.ExampleAssembler;
import com.manpou.procurement.common.exception.BusinessException;
import com.manpou.procurement.common.filter.TraceFilter;
import com.manpou.procurement.domain.model.Example;
import com.manpou.procurement.domain.repository.ExampleRepository;
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
public class ExampleUseCase {

    private final ExampleRepository exampleRepository;
    private final ExampleAssembler exampleAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<ExamplePageQuery> pageQuery(ExampleQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Example> page = exampleRepository.findAllByIsDeletedFalse(pageRequest);
        return page.map(exampleAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ExamplePageQuery getById(Long id) {
        Example entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Example", id));
        return exampleAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(ExampleCreateCmd cmd) {
        Example entity = exampleAssembler.toEntity(cmd);
        Example saved = exampleRepository.save(entity);
        log.info("[Example] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, ExampleUpdateCmd cmd) {
        Example entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Example", id));
        exampleAssembler.copyToEntity(cmd, entity);
        exampleRepository.save(entity);
        log.info("[Example] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        Example entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Example", id));
        entity.markDeleted();
        exampleRepository.save(entity);
        log.info("[Example] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
