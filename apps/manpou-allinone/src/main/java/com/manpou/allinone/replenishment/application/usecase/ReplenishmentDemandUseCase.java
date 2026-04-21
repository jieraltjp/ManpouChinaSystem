package com.manpou.allinone.replenishment.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.replenishment.application.assembler.ReplenishmentDemandAssembler;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
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
public class ReplenishmentDemandUseCase {

    private final ReplenishmentDemandRepository demandRepository;
    private final ReplenishmentDemandAssembler assembler;

    @Transactional(readOnly = true)
    public Page<ReplenishmentDemandPageQuery> pageQuery(ReplenishmentDemandQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<ReplenishmentDemand> page;
        if (query.getStatus() != null) {
            page = demandRepository.findByStatusAndIsDeletedFalse(query.getStatus(), pageRequest);
        } else if (query.getDemandType() != null) {
            page = demandRepository.findByDemandTypeAndIsDeletedFalse(query.getDemandType(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = demandRepository.findByProductCodeAndIsDeletedFalse(query.getProductCode(), pageRequest);
        } else {
            page = demandRepository.findAllByIsDeletedFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public ReplenishmentDemandPageQuery getById(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(ReplenishmentDemandCreateCmd cmd) {
        ReplenishmentDemand entity = assembler.toEntity(cmd);
        ReplenishmentDemand saved = demandRepository.save(entity);
        log.info("[ReplenishmentDemand] created, traceId={}, id={}, demandCode={}, type={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(), saved.getDemandType());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ReplenishmentDemandUpdateCmd cmd) {
        ReplenishmentDemand entity = demandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        assembler.copyToEntity(cmd, entity);
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] updated, traceId={}, id={}, status={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getStatus());
    }

    /**
     * 转为采购单。
     * 将 demand 状态推进为 CONVERTED，记录关联的 procurementId。
     */
    @Transactional
    public void convertToProcurement(Long demandId, Long procurementId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndIsDeletedFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));
        entity.convertToProcurement(procurementId);
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] converted to procurement, traceId={}, demandId={}, procurementId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementId);
    }

    @Transactional
    public void delete(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        if (entity.getStatus() != DemandStatus.PENDING) {
            throw BusinessException.invalidParam("仅待确认状态可删除");
        }
        entity.markDeleted();
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
