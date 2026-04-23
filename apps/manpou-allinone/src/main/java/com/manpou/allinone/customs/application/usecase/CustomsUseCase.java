package com.manpou.allinone.customs.application.usecase;

import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.assembler.CustomsAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.customs.domain.model.CustomsExample;
import com.manpou.allinone.customs.domain.repository.CustomsRepository;
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
public class CustomsUseCase {

    private final CustomsRepository customsRepository;
    private final CustomsAssembler customsAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<CustomsPageQuery> pageQuery(CustomsQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<CustomsExample> page = customsRepository.findAllByDeletedIsFalse(pageRequest);
        return page.map(customsAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public CustomsPageQuery getById(Long id) {
        CustomsExample entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("CustomsExample", id));
        return customsAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(CustomsCreateCmd cmd) {
        CustomsExample entity = customsAssembler.toEntity(cmd);
        CustomsExample saved = customsRepository.save(entity);
        log.info("[CustomsExample] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, CustomsUpdateCmd cmd) {
        CustomsExample entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("CustomsExample", id));
        customsAssembler.copyToEntity(cmd, entity);
        customsRepository.save(entity);
        log.info("[CustomsExample] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        CustomsExample entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("CustomsExample", id));
        entity.markDeleted();
        customsRepository.save(entity);
        log.info("[CustomsExample] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
