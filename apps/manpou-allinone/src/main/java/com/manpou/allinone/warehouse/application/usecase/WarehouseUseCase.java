package com.manpou.allinone.warehouse.application.usecase;

import com.manpou.allinone.warehouse.application.dto.WarehouseCreateCmd;
import com.manpou.allinone.warehouse.application.dto.WarehousePageQuery;
import com.manpou.allinone.warehouse.application.dto.WarehouseQuery;
import com.manpou.allinone.warehouse.application.dto.WarehouseUpdateCmd;
import com.manpou.allinone.warehouse.application.assembler.WarehouseAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.warehouse.domain.model.WarehouseExample;
import com.manpou.allinone.warehouse.domain.repository.WarehouseRepository;
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
public class WarehouseUseCase {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseAssembler warehouseAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<WarehousePageQuery> pageQuery(WarehouseQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<WarehouseExample> page = warehouseRepository.findAllByIsDeletedFalse(pageRequest);
        return page.map(warehouseAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public WarehousePageQuery getById(Long id) {
        WarehouseExample entity = warehouseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("WarehouseExample", id));
        return warehouseAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(WarehouseCreateCmd cmd) {
        WarehouseExample entity = warehouseAssembler.toEntity(cmd);
        WarehouseExample saved = warehouseRepository.save(entity);
        log.info("[WarehouseExample] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, WarehouseUpdateCmd cmd) {
        WarehouseExample entity = warehouseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("WarehouseExample", id));
        warehouseAssembler.copyToEntity(cmd, entity);
        warehouseRepository.save(entity);
        log.info("[WarehouseExample] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        WarehouseExample entity = warehouseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("WarehouseExample", id));
        entity.markDeleted();
        warehouseRepository.save(entity);
        log.info("[WarehouseExample] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
