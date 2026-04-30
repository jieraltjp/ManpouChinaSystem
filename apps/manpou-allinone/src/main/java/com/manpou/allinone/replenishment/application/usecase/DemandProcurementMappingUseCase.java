package com.manpou.allinone.replenishment.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.application.assembler.DemandProcurementMappingAssembler;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingCreateCmd;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingPageQuery;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingQuery;
import com.manpou.allinone.replenishment.domain.model.DemandProcurementMapping;
import com.manpou.allinone.replenishment.domain.model.MappingStatus;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.DemandProcurementMappingRepository;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 需求-采购分配映射用例服务（SPEC-B11 §12 P1）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandProcurementMappingUseCase {

    private final DemandProcurementMappingRepository mappingRepository;
    private final DemandProcurementMappingAssembler assembler;
    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<DemandProcurementMappingPageQuery> pageQuery(DemandProcurementMappingQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<DemandProcurementMapping> page;
        if (query.getDemandId() != null) {
            page = mappingRepository.findByDemandIdAndDeletedIsFalse(query.getDemandId(), pageRequest);
        } else if (query.getProcurementId() != null) {
            List<DemandProcurementMapping> list = mappingRepository
                    .findByProcurementIdAndDeletedIsFalse(query.getProcurementId());
            int start = (int) pageRequest.getOffset();
            int end = Math.min(start + pageRequest.getPageSize(), list.size());
            List<DemandProcurementMapping> paged = start < list.size() ? list.subList(start, end) : List.of();
            page = new org.springframework.data.domain.PageImpl<>(paged, pageRequest, list.size());
        } else if (query.getStatus() != null) {
            page = mappingRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else {
            page = mappingRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public DemandProcurementMappingPageQuery getById(Long id) {
        DemandProcurementMapping entity = mappingRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DemandProcurementMapping", id));
        return assembler.toDto(entity);
    }

    /**
     * 创建分配映射（关联需求单与采购单）。
     * 含子货号一致性校验（Q6 规则）。
     */
    @Transactional
    public Long create(DemandProcurementMappingCreateCmd cmd) {
        if (cmd.getDemandId() == null || cmd.getProcurementId() == null) {
            throw BusinessException.invalidParam("需求单ID和采购单ID不能为空");
        }
        if (cmd.getAllocatedQuantity() == null || cmd.getAllocatedQuantity() <= 0) {
            throw BusinessException.invalidParam("分配数量必须大于0");
        }

        // 唯一约束检查
        if (mappingRepository.existsByDemandIdAndProcurementIdAndDeletedIsFalse(
                cmd.getDemandId(), cmd.getProcurementId())) {
            throw BusinessException.conflict("该需求单与采购单的映射已存在");
        }

        // 子货号一致性校验（Q6）
        ReplenishmentDemand demand = demandRepository.findByIdAndDeletedIsFalse(cmd.getDemandId())
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", cmd.getDemandId()));
        Procurement procurement = procurementRepository.findByIdAndDeletedIsFalse(cmd.getProcurementId())
                .orElseThrow(() -> BusinessException.notFound("Procurement", cmd.getProcurementId()));

        if (demand.getSubProductCode() != null && procurement.getSubProductCode() != null
                && !demand.getSubProductCode().equals(procurement.getSubProductCode())) {
            throw BusinessException.invalidParam(
                    "子货号不一致，无法关联：需求单[" + demand.getSubProductCode()
                            + "] ≠ 采购单[" + procurement.getSubProductCode() + "]");
        }

        DemandProcurementMapping entity = assembler.toEntity(cmd);
        DemandProcurementMapping saved = mappingRepository.save(entity);

        log.info("[DemandProcurementMapping] created, traceId={}, id={}, demandId={}, procurementId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY),
                saved.getId(),
                saved.getDemandId(),
                saved.getProcurementId());
        return saved.getId();
    }

    /**
     * 取消分配。
     */
    @Transactional
    public void cancel(Long id) {
        DemandProcurementMapping entity = mappingRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DemandProcurementMapping", id));
        entity.cancel();
        mappingRepository.save(entity);

        log.info("[DemandProcurementMapping] cancelled, traceId={}, id={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 推进映射状态（由事件监听器调用）。
     * 当验货合格量 >= allocatedQuantity 时推进为已完成。
     */
    @Transactional
    public void advanceIfCompleted(Long id) {
        DemandProcurementMapping entity = mappingRepository.findByIdAndDeletedIsFalse(id)
                .orElse(null);
        if (entity == null || entity.getStatus() != MappingStatus.进行中) {
            return;
        }
        // 此处由 MappingStatusAutoUpdater 注入后调用，细节在 SPEC-B11 §9.1
        // 实际数量由调用方通过事件传入，此处仅做状态推进
        entity.markCompleted();
        mappingRepository.save(entity);

        log.info("[DemandProcurementMapping] completed, traceId={}, id={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
