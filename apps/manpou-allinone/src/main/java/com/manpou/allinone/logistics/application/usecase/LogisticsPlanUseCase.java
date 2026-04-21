package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.assembler.LogisticsPlanAssembler;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanUpdateCmd;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsPlanUseCase {

    private final LogisticsPlanRepository logisticsPlanRepository;
    private final LogisticsPlanAssembler logisticsPlanAssembler;

    @Transactional(readOnly = true)
    public Page<LogisticsPlanPageQuery> pageQuery(LogisticsPlanQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<LogisticsPlan> page;
        if (query.getStatus() != null) {
            page = logisticsPlanRepository.findByStatusAndIsDeletedFalse(query.getStatus(), pageRequest);
        } else if (query.getPlanType() != null) {
            page = logisticsPlanRepository.findByPlanTypeAndIsDeletedFalse(query.getPlanType(), pageRequest);
        } else if (query.getProcurementId() != null) {
            page = logisticsPlanRepository.findByProcurementIdAndIsDeletedFalse(query.getProcurementId(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = logisticsPlanRepository.findByProductCodeAndIsDeletedFalse(query.getProductCode(), pageRequest);
        } else {
            page = logisticsPlanRepository.findAllByIsDeletedFalse(pageRequest);
        }
        return page.map(logisticsPlanAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public LogisticsPlanPageQuery getById(Long id) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        return logisticsPlanAssembler.toDto(entity);
    }

    @Transactional
    public Long create(LogisticsPlanCreateCmd cmd) {
        LogisticsPlan entity = logisticsPlanAssembler.toEntity(cmd);
        LogisticsPlan saved = logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] created, traceId={}, id={}, planCode={}, planType={}",
                null, saved.getId(), saved.getPlanCode(), saved.getPlanType());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, LogisticsPlanUpdateCmd cmd) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("logistics.cannot_modify_delivered", "调配计划已完成，禁止修改");
        }
        logisticsPlanAssembler.copyUpdate(cmd, entity);
        logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] updated, traceId={}, id={}, status={}", null, id, entity.getStatus());
    }

    @Transactional
    public void delete(Long id) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("logistics.cannot_delete_delivered", "已完成/运输中的调配计划禁止删除");
        }
        entity.markDeleted();
        logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] deleted, traceId={}, id={}", null, id);
    }
}
