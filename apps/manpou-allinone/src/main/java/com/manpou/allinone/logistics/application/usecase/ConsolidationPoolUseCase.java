package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.assembler.ConsolidationPoolAssembler;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolCreateCmd;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolPageQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolUpdateCmd;
import com.manpou.allinone.logistics.domain.event.PoolReadyToLoadEvent;
import com.manpou.allinone.logistics.domain.model.ConsolidationPool;
import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.ConsolidationPoolRepository;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidationPoolUseCase {

    private final ConsolidationPoolRepository poolRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final ConsolidationPoolAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<ConsolidationPoolPageQuery> pageQuery(ConsolidationPoolQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ConsolidationPool> page;
        if (query.getStatus() != null) {
            page = poolRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else if (query.getDestinationPort() != null && !query.getDestinationPort().isBlank()) {
            page = poolRepository.findByDestinationPortAndDeletedIsFalse(query.getDestinationPort(), pageRequest);
        } else {
            page = poolRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public ConsolidationPoolPageQuery getById(Long id) {
        ConsolidationPool entity = poolRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(ConsolidationPoolCreateCmd cmd) {
        ConsolidationPool entity = assembler.toEntity(cmd);
        ConsolidationPool saved = poolRepository.save(entity);
        log.info("[ConsolidationPool] created, traceId={}, id={}, poolCode={}, destinationPort={}",
                null, saved.getId(), saved.getPoolCode(), saved.getDestinationPort());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ConsolidationPoolUpdateCmd cmd) {
        ConsolidationPool entity = poolRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));
        if (entity.getStatus().isTerminal()) {
            throw new BusinessException("logistics.pool_cannot_modify", "已出港状态禁止修改");
        }
        assembler.copyUpdate(cmd, entity);
        poolRepository.save(entity);
        log.info("[ConsolidationPool] updated, traceId={}, id={}, status={}", null, id, entity.getStatus());
    }

    @Transactional
    public void delete(Long id) {
        ConsolidationPool entity = poolRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));
        if (entity.getStatus() != ConsolidationPoolStatus.OPEN) {
            throw new BusinessException("logistics.pool_cannot_delete", "仅 OPEN 状态允许删除");
        }
        entity.markDeleted();
        poolRepository.save(entity);
        log.info("[ConsolidationPool] deleted, traceId={}, id={}", null, id);
    }

    /**
     * 将 LogisticsPlan 加入拼柜池（OPEN 状态时可用）。
     */
    @Transactional
    public void addPlan(Long poolId, Long logisticsPlanId) {
        ConsolidationPool pool = poolRepository.findByIdAndDeletedIsFalse(poolId)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));
        LogisticsPlan plan = logisticsPlanRepository.findByIdAndDeletedIsFalse(logisticsPlanId)
                .orElseThrow(() -> new BusinessException("logistics.plan_not_found", "调配计划不存在"));

        pool.addPlan(plan.getCargoVolumeCbm(), plan.getCargoWeightKg());
        poolRepository.save(pool);

        // 绑定 plan → pool
        plan.setPoolId(poolId);
        logisticsPlanRepository.save(plan);

        log.info("[ConsolidationPool] plan added, traceId={}, poolId={}, planId={}, totalCbm={}",
                null, poolId, logisticsPlanId, pool.getTotalCbm());

        // 达到阈值则触发装柜事件
        if (pool.isReadyToLoad() && pool.getStatus() == ConsolidationPoolStatus.OPEN) {
            pool.advanceStatus(ConsolidationPoolStatus.PENDING);
            poolRepository.save(pool);
            eventPublisher.publishEvent(new PoolReadyToLoadEvent(
                    this, pool.getId(), pool.getPoolCode(), pool.getDestinationPort()));
            log.info("[ConsolidationPool] ready to load, poolId={}, totalCbm={}, threshold={}",
                    poolId, pool.getTotalCbm(), pool.getContainerThresholdCbm());
        }
    }

    /**
     * 从拼柜池移除 LogisticsPlan。
     */
    @Transactional
    public void removePlan(Long poolId, Long logisticsPlanId) {
        ConsolidationPool pool = poolRepository.findByIdAndDeletedIsFalse(poolId)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));
        LogisticsPlan plan = logisticsPlanRepository.findByIdAndDeletedIsFalse(logisticsPlanId)
                .orElseThrow(() -> new BusinessException("logistics.plan_not_found", "调配计划不存在"));

        pool.removePlan(plan.getCargoVolumeCbm(), plan.getCargoWeightKg());
        poolRepository.save(pool);

        plan.setPoolId(null);
        logisticsPlanRepository.save(plan);

        log.info("[ConsolidationPool] plan removed, traceId={}, poolId={}, planId={}, totalCbm={}",
                null, poolId, logisticsPlanId, pool.getTotalCbm());
    }
}
