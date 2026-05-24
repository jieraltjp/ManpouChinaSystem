package com.manpou.allinone.offlineorder.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.offlineorder.application.assembler.OfflineOrderAssembler;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderCreateCmd;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderPageVO;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderQuery;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderUpdateCmd;
import com.manpou.allinone.offlineorder.domain.model.OfflineOrder;
import com.manpou.allinone.offlineorder.domain.repository.OfflineOrderRepository;
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
public class OfflineOrderUseCase {

    private final OfflineOrderRepository repository;
    private final OfflineOrderAssembler assembler;

    @Transactional(readOnly = true)
    public Page<OfflineOrderPageVO> pageQuery(OfflineOrderQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<OfflineOrder> page;
        if (query.getCode() != null && !query.getCode().isBlank()) {
            page = repository.findByCodeContaining(query.getCode(), pageRequest);
        } else if (query.getItemName() != null && !query.getItemName().isBlank()) {
            page = repository.findByItemNameContaining(query.getItemName(), pageRequest);
        } else if (query.getFactory() != null && !query.getFactory().isBlank()) {
            page = repository.findByFactoryContaining(query.getFactory(), pageRequest);
        } else if (query.getArrival() != null && !query.getArrival().isBlank()) {
            page = repository.findByArrival(query.getArrival(), pageRequest);
        } else {
            page = repository.findAll(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public OfflineOrderPageVO getById(Long id) {
        OfflineOrder entity = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("OfflineOrder", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(OfflineOrderCreateCmd cmd) {
        OfflineOrder entity = assembler.toEntity(cmd);
        OfflineOrder saved = repository.save(entity);
        log.info("[OfflineOrder] created, traceId={}, id={}, code={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getCode());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, OfflineOrderUpdateCmd cmd) {
        OfflineOrder entity = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("OfflineOrder", id));
        assembler.copyToEntity(cmd, entity);
        repository.save(entity);
        log.info("[OfflineOrder] updated, traceId={}, id={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.findById(id).isPresent()) {
            throw BusinessException.notFound("OfflineOrder", id);
        }
        repository.deleteById(id);
        log.info("[OfflineOrder] deleted, traceId={}, id={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}