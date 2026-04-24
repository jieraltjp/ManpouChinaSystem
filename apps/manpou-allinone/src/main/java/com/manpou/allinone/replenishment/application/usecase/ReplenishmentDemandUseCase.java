package com.manpou.allinone.replenishment.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.application.assembler.ReplenishmentDemandAssembler;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandCmd;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandResponse;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * 补货需求用例服务（v2.0.0）。
 * 一条 Demand = 一个子货号（商品唯一标识 = 主货号+子货号）。
 * 转采购：1 Demand → 1 Procurement（一对一）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishmentDemandUseCase {

    private final ReplenishmentDemandRepository demandRepository;
    private final ReplenishmentDemandAssembler assembler;
    private final ProcurementRepository procurementRepository;
    private final FactoryRepository factoryRepository;
    private final ProcurementUseCase procurementUseCase;
    private final ProcurementAssembler procurementAssembler;

    @Transactional(readOnly = true)
    public Page<ReplenishmentDemandPageQuery> pageQuery(ReplenishmentDemandQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<ReplenishmentDemand> page;
        if (query.getStatus() != null) {
            page = demandRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else if (query.getDemandType() != null) {
            page = demandRepository.findByDemandTypeAndDeletedIsFalse(query.getDemandType(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = demandRepository.findByProductCodeAndDeletedIsFalse(query.getProductCode(), pageRequest);
        } else {
            page = demandRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public ReplenishmentDemandPageQuery getById(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(ReplenishmentDemandCreateCmd cmd) {
        ReplenishmentDemand entity = assembler.toEntity(cmd);
        ReplenishmentDemand saved = demandRepository.save(entity);
        log.info("[ReplenishmentDemand] created, traceId={}, id={}, demandCode={}, type={}, subProductCode={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(),
                saved.getDemandType(), saved.getSubProductCode());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ReplenishmentDemandUpdateCmd cmd) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        assembler.copyToEntity(cmd, entity);
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] updated, traceId={}, id={}, status={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getStatus());
    }

    /**
     * 转采购（v2.0.0）。
     * 一条 Demand → 一条 Procurement（1:1）。
     */
    @Transactional
    public ConvertDemandResponse convertToProcurement(Long demandId, ConvertDemandCmd cmd) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));

        if (entity.getStatus() != DemandStatus.PENDING) {
            throw BusinessException.invalidParam("需求单已处理，无法转采购");
        }

        if (entity.getQuantity() == null || entity.getQuantity() <= 0) {
            throw BusinessException.invalidParam("需求数量无效，无法转采购");
        }

        Factory factory = factoryRepository.findByIdAndDeletedIsFalse(cmd.getFactoryId())
                .orElseThrow(() -> BusinessException.notFound("Factory", cmd.getFactoryId()));

        ProcurementCreateCmd pCmd = new ProcurementCreateCmd();
        pCmd.setFactoryId(cmd.getFactoryId());
        pCmd.setProductCode(entity.getProductCode());
        pCmd.setSubProductCode(entity.getSubProductCode());
        pCmd.setQuantity(entity.getQuantity());
        pCmd.setDestination(entity.getDestination());
        pCmd.setJapanLead(entity.getJapanLead());
        pCmd.setPriceRmb(BigDecimal.ZERO);
        pCmd.setExchangeRate(BigDecimal.ONE);
        pCmd.setTaxPoint(new BigDecimal("1.1"));
        pCmd.setLinkedDemandId(demandId);

        Long procurementId = procurementUseCase.create(pCmd);
        entity.markAsConverted(procurementId);
        demandRepository.save(entity);

        log.info("[ReplenishmentDemand] converted to procurement, traceId={}, demandId={}, procurementId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementId);

        return ConvertDemandResponse.builder()
                .demandStatus(DemandStatus.CONVERTED)
                .linkedProcurementId(procurementId)
                .build();
    }

    /**
     * 撤销转换（v2.0.0）。
     * 删除关联 Procurement，回滚 Demand → PENDING。
     */
    @Transactional
    public void revertConversion(Long demandId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));

        if (entity.getStatus() != DemandStatus.CONVERTED) {
            throw BusinessException.invalidParam("需求单未转采购，无需撤销");
        }

        Long procurementId = entity.getLinkedProcurementId();
        if (procurementId != null) {
            Procurement procurement = procurementRepository.findByIdAndDeletedIsFalse(procurementId)
                    .orElse(null);
            if (procurement != null && procurement.getStatus().isTerminal()) {
                throw BusinessException.invalidParam(
                        "关联发注单 [" + procurementId + "] 已推进至终态，禁止撤销");
            }
            procurementRepository.deleteById(procurementId);
        }

        entity.revertConversion();
        demandRepository.save(entity);

        log.info("[ReplenishmentDemand] conversion reverted, traceId={}, demandId={}, deletedProcurementId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementId);
    }

    /**
     * 查看关联的采购单（v2.0.0）。
     */
    @Transactional(readOnly = true)
    public ProcurementPageQuery getLinkedProcurement(Long demandId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));
        if (entity.getLinkedProcurementId() == null) {
            return null;
        }
        return procurementRepository.findByIdAndDeletedIsFalse(entity.getLinkedProcurementId())
                .map(procurementAssembler::toDto)
                .orElse(null);
    }

    /**
     * 批量查询关联的采购单列表（用于多选 Demand 的场景）。
     */
    @Transactional(readOnly = true)
    public List<ProcurementPageQuery> getLinkedProcurements(List<Long> demandIds) {
        List<ReplenishmentDemand> demands = demandRepository.findAllByIdInAndDeletedIsFalse(demandIds);
        List<Long> procurementIds = demands.stream()
                .map(ReplenishmentDemand::getLinkedProcurementId)
                .filter(id -> id != null)
                .toList();
        if (procurementIds.isEmpty()) {
            return List.of();
        }
        return procurementRepository.findAllByIdInAndDeletedIsFalse(procurementIds)
                .stream()
                .map(procurementAssembler::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        if (entity.getStatus() != DemandStatus.PENDING) {
            throw BusinessException.invalidParam("仅待确认状态可删除");
        }
        entity.markDeleted();
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /** 去重目的地列表 */
    @Transactional(readOnly = true)
    public List<String> findDistinctDestinations() {
        return demandRepository.findDistinctDestinations();
    }

    /** 去重日本担当列表 */
    @Transactional(readOnly = true)
    public List<String> findDistinctJapanLeads() {
        return demandRepository.findDistinctJapanLeads();
    }
}
