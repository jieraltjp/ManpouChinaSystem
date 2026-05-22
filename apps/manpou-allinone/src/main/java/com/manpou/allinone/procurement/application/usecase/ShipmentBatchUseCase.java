package com.manpou.allinone.procurement.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.procurement.application.assembler.ShipmentBatchAssembler;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchCreateCmd;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchPageQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchUpdateCmd;
import com.manpou.allinone.procurement.domain.event.ShipmentBatchCreatedEvent;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.procurement.domain.repository.ShipmentBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 出货批次用例服务（SPEC-B11 §12 P1）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentBatchUseCase {

    private final ShipmentBatchRepository shipmentBatchRepository;
    private final ProcurementRepository procurementRepository;
    private final ShipmentBatchAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<ShipmentBatchPageQuery> pageQuery(ShipmentBatchQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<ShipmentBatch> page;
        if (query.getProcurementId() != null) {
            page = shipmentBatchRepository.findByProcurementIdAndDeletedIsFalse(query.getProcurementId(), pageRequest);
        } else if (query.getStatus() != null) {
            page = shipmentBatchRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else {
            page = shipmentBatchRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ShipmentBatchPageQuery getById(Long id) {
        ShipmentBatch entity = shipmentBatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ShipmentBatch", id));
        return assembler.toDto(entity);
    }

    /**
     * 创建出货批次。
     * 自动生成批次编号前缀，序号由前端传参或自动生成。
     */
    @Transactional
    public Long create(ShipmentBatchCreateCmd cmd) {
        if (cmd.getProcurementId() == null) {
            throw BusinessException.invalidParam("采购单ID不能为空");
        }
        if (cmd.getShipmentQuantity() == null || cmd.getShipmentQuantity() <= 0) {
            throw BusinessException.invalidParam("出货数量必须大于0");
        }
        ShipmentBatch entity = assembler.toEntity(cmd);
        ShipmentBatch saved = shipmentBatchRepository.save(entity);

        // SPEC-B13: 发布出货批次创建事件，触发老厂家免验自动创建
        Long factoryId = resolveFactoryId(saved.getProcurementId());
        eventPublisher.publishEvent(new ShipmentBatchCreatedEvent(
                saved.getId(), saved.getProcurementId(), factoryId));

        log.info("[ShipmentBatch] created, traceId={}, id={}, procurementId={}, quantity={}, factoryId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY),
                saved.getId(),
                saved.getProcurementId(),
                saved.getShipmentQuantity(),
                factoryId);
        return saved.getId();
    }

    /**
     * 更新出货批次（部分更新，含状态推进）。
     */
    @Transactional
    public void update(Long id, ShipmentBatchUpdateCmd cmd) {
        ShipmentBatch entity = shipmentBatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ShipmentBatch", id));
        assembler.copyToEntity(cmd, entity);
        shipmentBatchRepository.save(entity);

        log.info("[ShipmentBatch] updated, traceId={}, id={}, status={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getStatus());
    }

    /**
     * 删除出货批次（逻辑删除）。
     * 仅待验货状态可删除。
     */
    @Transactional
    public void delete(Long id) {
        ShipmentBatch entity = shipmentBatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ShipmentBatch", id));
        if (!entity.getStatus().canDelete()) {
            throw BusinessException.invalidParam(
                    "仅待验货状态可删除，当前状态：" + entity.getStatus());
        }
        entity.markDeleted();
        shipmentBatchRepository.save(entity);

        log.info("[ShipmentBatch] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 关联验货记录到出货批次（1:N，幂等）。
     * SPEC-B11 §10.1 link-qc API。
     */
    @Transactional
    public void linkQc(Long batchId, Long qcRecordId) {
        ShipmentBatch batch = shipmentBatchRepository.findByIdAndDeletedIsFalse(batchId)
                .orElseThrow(() -> BusinessException.notFound("ShipmentBatch", batchId));
        batch.advanceStatusOnQcLink();
        shipmentBatchRepository.save(batch);

        log.info("[ShipmentBatch] linkQc, traceId={}, batchId={}, qcRecordId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), batchId, qcRecordId);
    }

    /** SPEC-B13: 通过 procurementId 解析 factoryId。 */
    private Long resolveFactoryId(Long procurementId) {
        if (procurementId == null) return null;
        return procurementRepository.findByIdAndDeletedIsFalse(procurementId)
                .map(Procurement::getFactoryId)
                .orElse(null);
    }
}
