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
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.application.assembler.ReplenishmentDemandAssembler;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandCmd;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandResponse;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.application.dto.SubProductItemDto;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.LinkedDemandItem;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 补货需求用例服务（v1.6.0）。
 * 批量转采购：每个 SubProductItem 生成一条 Procurement。
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
        log.info("[ReplenishmentDemand] created, traceId={}, id={}, demandCode={}, type={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getDemandCode(), saved.getDemandType());
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
     * 批量转采购（v1.6.0）。
     * 每个 SubProductItem 生成一条 Procurement，返回所有生成的 ID 列表。
     */
    @Transactional
    public ConvertDemandResponse convertToProcurement(Long demandId, ConvertDemandCmd cmd) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));

        if (entity.getStatus() != DemandStatus.PENDING) {
            throw BusinessException.invalidParam("需求单已处理，无法转采购");
        }

        List<SubProductItem> subItems = assembler.parseSubProductItems(entity.getSubProductItemsRaw());
        if (subItems.isEmpty()) {
            throw BusinessException.invalidParam("子货号明细为空，无法转采购");
        }

        // 校验工厂
        Factory factory = factoryRepository.findByIdAndDeletedIsFalse(cmd.getFactoryId())
                .orElseThrow(() -> BusinessException.notFound("Factory", cmd.getFactoryId()));

        // 批量生成 Procurement
        List<Long> procurementIds = new ArrayList<>();
        List<LinkedDemandItem> linkedItems = new ArrayList<>();
        for (int i = 0; i < subItems.size(); i++) {
            SubProductItem item = subItems.get(i);
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw BusinessException.invalidParam(
                        String.format("子货号 [%s] 数量无效（%s），无法转采购",
                                item.getSubCode(), item.getQuantity()));
            }
            ProcurementCreateCmd pCmd = new ProcurementCreateCmd();
            pCmd.setFactoryId(cmd.getFactoryId());
            pCmd.setProductCode(entity.getProductCode());
            pCmd.setSubProductCode(item.getSubCode());
            pCmd.setQuantity(item.getQuantity());
            pCmd.setDestination(item.getDestination());
            pCmd.setJapanLead(entity.getJapanLead());
            pCmd.setPriceRmb(BigDecimal.ZERO);
            pCmd.setExchangeRate(BigDecimal.ONE);
            pCmd.setTaxPoint(new BigDecimal("1.1"));
            pCmd.setLinkedDemandId(demandId);
            pCmd.setLinkedDemandItemId((long) i);
            Long procurementId = procurementUseCase.create(pCmd);
            procurementIds.add(procurementId);
            linkedItems.add(new LinkedDemandItem(procurementId, item.getSubCode()));
        }

        // 更新 Demand 状态
        assembler.applyConvertedState(entity, linkedItems);
        demandRepository.save(entity);

        log.info("[ReplenishmentDemand] batch converted to procurement, traceId={}, demandId={}, count={}, procurementIds={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementIds.size(), procurementIds);

        return ConvertDemandResponse.builder()
                .demandStatus(DemandStatus.CONVERTED)
                .linkedProcurementIds(procurementIds)
                .build();
    }

    /**
     * 批量撤销转换（v1.6.0）。
     * 删除所有关联 Procurement，回滚 Demand → PENDING。
     * 条件：关联 Procurement 均未推进至终态。
     */
    @Transactional
    public void revertConversion(Long demandId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));

        if (entity.getStatus() != DemandStatus.CONVERTED) {
            throw BusinessException.invalidParam("需求单未转采购，无需撤销");
        }

        List<LinkedDemandItem> linkedItems = assembler.parseLinkedDemandItems(entity.getLinkedDemandItemsRaw());
        if (linkedItems.isEmpty()) {
            entity.revertConversion();
            demandRepository.save(entity);
            return;
        }

        List<Long> procurementIds = linkedItems.stream()
                .map(LinkedDemandItem::getLinkedProcurementId)
                .toList();

        // 批量检查状态
        List<Procurement> procurements = procurementRepository.findAllByIdInAndDeletedIsFalse(procurementIds);
        for (Procurement p : procurements) {
            if (p.getStatus().isTerminal()) {
                throw BusinessException.invalidParam(
                        "关联发注单 [" + p.getId() + "] 已推进至终态，禁止撤销");
            }
        }

        // 批量删除
        for (Long pid : procurementIds) {
            procurementRepository.deleteById(pid);
        }

        entity.revertConversion();
        demandRepository.save(entity);

        log.info("[ReplenishmentDemand] conversion reverted, traceId={}, demandId={}, deletedProcurementIds={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementIds);
    }

    /**
     * 查看关联的采购单列表（v1.6.0）。
     */
    @Transactional(readOnly = true)
    public List<ProcurementPageQuery> getLinkedProcurements(Long demandId) {
        return procurementRepository.findByLinkedDemandIdAndDeletedIsFalse(demandId)
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
}
