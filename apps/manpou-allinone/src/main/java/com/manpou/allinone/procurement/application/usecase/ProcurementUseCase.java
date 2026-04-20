package com.manpou.allinone.procurement.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 发注单用例服务。
 * 负责编排业务操作，不含领域逻辑。
 * 领域逻辑（状态机、价格计算）封装在 Procurement 实体中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementUseCase {

    private final ProcurementRepository procurementRepository;
    private final ProcurementAssembler procurementAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<ProcurementPageQuery> pageQuery(ProcurementQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Procurement> page;
        if (query.getStatus() != null) {
            page = procurementRepository.findByStatusAndIsDeletedFalse(query.getStatus(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = procurementRepository.findByProductCodeAndIsDeletedFalse(query.getProductCode(), pageRequest);
        } else if (query.getCustomerCompany() != null && !query.getCustomerCompany().isBlank()) {
            page = procurementRepository.findByCustomerCompanyAndIsDeletedFalse(query.getCustomerCompany(), pageRequest);
        } else {
            page = procurementRepository.findAllByIsDeletedFalse(pageRequest);
        }
        return page.map(procurementAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ProcurementPageQuery getById(Long id) {
        Procurement entity = procurementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));
        return procurementAssembler.toDto(entity);
    }

    /**
     * 创建发注单。
     */
    @Transactional
    public Long create(ProcurementCreateCmd cmd) {
        Procurement entity = procurementAssembler.toEntity(cmd);
        Procurement saved = procurementRepository.save(entity);
        log.info("[Procurement] created, traceId={}, id={}, productCode={}, estimatedPriceJpy={}",
                MDC.get(TraceFilter.TRACE_ID_KEY),
                saved.getId(),
                saved.getProductCode(),
                saved.getEstimatedPriceJpy());
        return saved.getId();
    }

    /**
     * 更新发注单（部分更新，含状态推进）。
     */
    @Transactional
    public void update(Long id, ProcurementUpdateCmd cmd) {
        Procurement entity = procurementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));
        procurementAssembler.copyToEntity(cmd, entity);
        procurementRepository.save(entity);
        log.info("[Procurement] updated, traceId={}, id={}, status={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getStatus());
    }

    /**
     * 逻辑删除。
     * 仅未定/発注待状态可删除。
     */
    @Transactional
    public void delete(Long id) {
        Procurement entity = procurementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));
        ShipmentStatus current = entity.getStatus();
        if (current != ShipmentStatus.未定 && current != ShipmentStatus.発注待) {
            throw BusinessException.invalidParam(
                    "仅未定/発注待状态可删除，当前状态：" + current);
        }
        entity.markDeleted();
        procurementRepository.save(entity);
        log.info("[Procurement] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
