package com.manpou.allinone.customs.application.usecase;

import com.manpou.allinone.customs.application.dto.CustomsBatchCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.assembler.CustomsAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 国内报关用例服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomsUseCase {

    private final DomesticCustomsRepository customsRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final CustomsAssembler customsAssembler;

    @Transactional(readOnly = true)
    public Page<CustomsPageQuery> pageQuery(CustomsQuery query) {
        int pageIndex = query.getPage() == null ? 0 : Math.max(0, query.getPage() - 1);
        PageRequest pageRequest = PageRequest.of(
                pageIndex,
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        // v1.3.0: containerNo 筛选支持（模糊匹配）
        if (query.getContainerNo() != null && !query.getContainerNo().isBlank()) {
            Page<DomesticCustomsRecord> page = customsRepository
                    .findByContainerNoContainingAndDeletedIsFalse(query.getContainerNo(), pageRequest);
            return page.map(customsAssembler::toDto);
        }
        Page<DomesticCustomsRecord> page = customsRepository.findAllByDeletedIsFalse(pageRequest);
        return page.map(customsAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public CustomsPageQuery getById(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        return customsAssembler.toDto(entity);
    }

    @Transactional
    public Long create(CustomsCreateCmd cmd) {
        DomesticCustomsRecord entity = customsAssembler.toEntity(cmd);
        DomesticCustomsRecord saved = customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] created, traceId={}, id={}, customsCode={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getCustomsCode());
        return saved.getId();
    }

    /**
     * 批量创建国内报关记录（v1.4.0）。
     * 根据 logisticsPlanIds 查询 LogisticsPlan 实体，自动填充 productCode/subProductCode/factoryId/quantity 等字段。
     * 一个 LogisticsPlan 对应一条 DomesticCustomsRecord。
     */
    @Transactional
    public List<Long> batchCreate(CustomsBatchCreateCmd cmd) {
        List<Long> createdIds = new ArrayList<>();
        for (Long logisticsPlanId : cmd.getLogisticsPlanIds()) {
            LogisticsPlan plan = logisticsPlanRepository.findById(logisticsPlanId)
                    .orElseThrow(() -> BusinessException.notFound("LogisticsPlan", logisticsPlanId));
            CustomsCreateCmd single = new CustomsCreateCmd();
            single.setContainerNo(cmd.getContainerNo());
            single.setLogisticsPlanId(logisticsPlanId);
            single.setProcurementId(plan.getProcurementId());
            single.setFactoryId(plan.getFactoryId());
            single.setProductCode(plan.getProductCode());
            single.setSubProductCode(plan.getSubProductCode());
            single.setQuantity(cmd.getQuantity() != null ? cmd.getQuantity() : plan.getQuantity());
            single.setEstimatedValueCny(cmd.getEstimatedValueCny());
            single.setRemarks(cmd.getRemarks());
            DomesticCustomsRecord entity = customsAssembler.toEntity(single);
            DomesticCustomsRecord saved = customsRepository.save(entity);
            createdIds.add(saved.getId());
            log.info("[DomesticCustomsRecord] batch-created, traceId={}, id={}, logisticsPlanId={}",
                    MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), logisticsPlanId);
        }
        return createdIds;
    }

    @Transactional
    public void update(Long id, CustomsUpdateCmd cmd) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        customsAssembler.copyToEntity(cmd, entity);
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void submit(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.submit();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] submitted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void clear(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.clear();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] cleared, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void reject(Long id, String reason) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.reject(reason);
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] rejected, traceId={}, id={}, reason={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, reason);
    }

    @Transactional
    public void delete(Long id) {
        DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
        entity.markDeleted();
        customsRepository.save(entity);
        log.info("[DomesticCustomsRecord] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 批量修改报关申报号（v2.0）。
     * 将选中记录的 customsDeclarationNo 统一更新为指定值。
     */
    @Transactional
    public int batchUpdateDeclarationNo(List<Long> ids, String declarationNo) {
        int count = 0;
        for (Long id : ids) {
            DomesticCustomsRecord entity = customsRepository.findByIdAndDeletedIsFalse(id)
                    .orElseThrow(() -> BusinessException.notFound("DomesticCustomsRecord", id));
            entity.setCustomsDeclarationNo(declarationNo);
            customsRepository.save(entity);
            count++;
        }
        log.info("[DomesticCustomsRecord] batch-update-declaration-no, traceId={}, count={}, declarationNo={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), count, declarationNo);
        return count;
    }
}
