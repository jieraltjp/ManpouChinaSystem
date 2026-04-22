package com.manpou.allinone.factory.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.factory.application.assembler.FactoryAssembler;
import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.application.dto.FactoryQuery;
import com.manpou.allinone.factory.application.dto.FactoryUpdateCmd;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryUseCase {

    private final FactoryRepository factoryRepository;
    private final ProcurementRepository procurementRepository;
    private final FactoryAssembler assembler;

    @Transactional(readOnly = true)
    public Page<FactoryPageQuery> pageQuery(FactoryQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Factory> page;
        if (query.getCooperationStatus() != null) {
            page = factoryRepository.findByCooperationStatusAndIsDeletedFalse(query.getCooperationStatus(), pageRequest);
        } else if (query.getFactoryName() != null && !query.getFactoryName().isBlank()) {
            page = factoryRepository.findByFactoryNameAndIsDeletedFalse(query.getFactoryName(), pageRequest);
        } else {
            page = factoryRepository.findAllByIsDeletedFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public FactoryPageQuery getById(Long id) {
        Factory entity = factoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Factory", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(FactoryCreateCmd cmd) {
        Factory entity = assembler.toEntity(cmd);
        Factory saved = factoryRepository.save(entity);
        log.info("[Factory] created, traceId={}, id={}, factoryCode={}, factoryName={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getFactoryCode(), saved.getFactoryName());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, FactoryUpdateCmd cmd) {
        Factory entity = factoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Factory", id));
        assembler.copyToEntity(cmd, entity);
        factoryRepository.save(entity);
        log.info("[Factory] updated, traceId={}, id={}, cooperationStatus={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getCooperationStatus());
    }

    /**
     * 删除工厂。
     * 校验规则：有未终态发注单（状态非"完了"/"退货"）关联时拒绝删除。
     */
    @Transactional
    public void delete(Long id) {
        Factory entity = factoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Factory", id));

        // 校验无未终态发注单关联
        if (procurementRepository.existsActiveByFactoryId(id)) {
            throw BusinessException.invalidParam("该工厂存在未终态发注单，无法删除");
        }

        entity.markDeleted();
        factoryRepository.save(entity);
        log.info("[Factory] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
