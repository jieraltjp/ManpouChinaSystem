package com.manpou.allinone.replenishment.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.common.port.ProductQueryPort;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
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

import java.util.List;

/**
 * 补货需求用例服务（v2.2.0）。
 * 一条 Demand = 一个子货号（商品唯一标识 = 主货号+子货号）。
 * Demand 由 Procurement 主动关联（反向关联），不再由 Demand 生成 Procurement。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishmentDemandUseCase {

    private final ReplenishmentDemandRepository demandRepository;
    private final ReplenishmentDemandAssembler assembler;
    private final ProcurementRepository procurementRepository;
    private final ProductQueryPort productQueryPort;
    private final ProcurementAssembler procurementAssembler;

    @Transactional(readOnly = true)
    public Page<ReplenishmentDemandPageQuery> pageQuery(ReplenishmentDemandQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<ReplenishmentDemand> page;
        if (query.getDemandType() != null) {
            page = demandRepository.findByDemandTypeAndDeletedIsFalse(query.getDemandType(), pageRequest);
        } else if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            page = demandRepository.findByProductCodeOrSubProductCodeFuzzy(query.getKeyword().trim(), pageRequest);
        } else {
            page = demandRepository.findAllByDeletedIsFalse(pageRequest);
        }
        // 批量补充 imageUrl（v2.1.0）
        var productCodes = page.getContent().stream()
                .map(ReplenishmentDemand::getProductCode)
                .distinct()
                .toList();
        java.util.Map<String, String> imageUrlMap = productQueryPort.findByMasterCodeIn(productCodes).stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getMasterCode(),
                        p -> p.getImageUrl() != null ? p.getImageUrl() : "",
                        (a, b) -> a
                ));
        return page.map(d -> {
            ReplenishmentDemandPageQuery dto = assembler.toDto(d);
            dto.setImageUrl(imageUrlMap.getOrDefault(d.getProductCode(), d.getImageUrl()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public ReplenishmentDemandPageQuery getById(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
        ReplenishmentDemandPageQuery dto = assembler.toDto(entity);
        // 补充 imageUrl（v2.1.0）
        if (dto.getImageUrl() == null) {
            productQueryPort.findByMasterCode(entity.getProductCode())
                    .ifPresent(p -> dto.setImageUrl(p.getImageUrl()));
        }
        return dto;
    }

    @Transactional
    public Long create(ReplenishmentDemandCreateCmd cmd) {
        ReplenishmentDemand entity = assembler.toEntity(cmd);
        // 创建时同步商品图片（v2.1.0）
        productQueryPort.findByMasterCode(cmd.getProductCode())
                .ifPresent(p -> entity.setImageUrl(p.getImageUrl()));
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
     * 关联到发注单（v2.2.0）。
     * 由 ProcurementPage 新建/编辑弹窗选择关联需求时调用。
     */
    @Transactional
    public void linkToProcurement(Long demandId, Long procurementId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));
        entity.markAsLinked(procurementId);
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] linked to procurement, traceId={}, demandId={}, procurementId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), demandId, procurementId);
    }

    /**
     * 取消关联（v2.2.0）。
     * 由 DemandPage 点击 CONFIRMED 标签，或由 ProcurementPage 取消关联时调用。
     */
    @Transactional
    public void unlinkProcurement(Long demandId) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(demandId)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", demandId));
        if (entity.getStatus() != DemandStatus.CONFIRMED) {
            throw BusinessException.invalidParam("当前需求单未关联发注单，无法取消关联");
        }
        entity.unlinkProcurement();
        demandRepository.save(entity);
        log.info("[ReplenishmentDemand] unlinked, traceId={}, demandId={}", MDC.get(TraceFilter.TRACE_ID_KEY), demandId);
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

    /**
     * 删除需求单（v2.2.0）。
     * 任何状态均可删除，无状态限制。
     */
    @Transactional
    public void delete(Long id) {
        ReplenishmentDemand entity = demandRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ReplenishmentDemand", id));
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
