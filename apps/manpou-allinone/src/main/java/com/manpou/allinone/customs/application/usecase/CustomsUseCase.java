package com.manpou.allinone.customs.application.usecase;

import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.assembler.CustomsAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 国内报关用例服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomsUseCase {

    private final DomesticCustomsRepository customsRepository;
    private final CustomsAssembler customsAssembler;

    @Transactional(readOnly = true)
    public Page<CustomsPageQuery> pageQuery(CustomsQuery query) {
        int pageIndex = query.getPage() == null ? 0 : Math.max(0, query.getPage() - 1);
        PageRequest pageRequest = PageRequest.of(
                pageIndex,
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<DomesticCustomsRecord> page = customsRepository.findAllByDeletedIsFalse(pageRequest);
        return page.map(customsAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public CustomsPageQuery getById(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        return customsAssembler.toDto(entity);
    }

    @Transactional
    public Long create(CustomsCreateCmd cmd) {
        DomesticCustomsRecord entity = customsAssembler.toEntity(cmd);
        DomesticCustomsRecord saved = customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] created, traceId={}, id={}, customsCode={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getCustomsCode());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, CustomsUpdateCmd cmd) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        customsAssembler.copyToEntity(cmd, entity);
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void submit(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.submit();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] submitted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void clear(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.clear();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] cleared, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void reject(Long id, String reason) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.reject(reason);
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] rejected, traceId={}, id={}, reason={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, reason);
    }

    @Transactional
    public void delete(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.markDeleted();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
