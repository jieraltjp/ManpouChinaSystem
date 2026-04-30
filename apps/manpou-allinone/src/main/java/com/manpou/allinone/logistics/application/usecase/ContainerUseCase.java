package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.assembler.ContainerAssembler;
import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.domain.model.Container;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.ConsolidationPool;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import com.manpou.allinone.logistics.domain.repository.ConsolidationPoolRepository;
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
public class ContainerUseCase {

    private final ContainerRepository containerRepository;
    private final ConsolidationPoolRepository poolRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final ContainerAssembler assembler;

    @Transactional(readOnly = true)
    public Page<ContainerPageQuery> pageQuery(ContainerQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Container> page;
        if (query.getStatus() != null) {
            page = containerRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else if (query.getPoolId() != null) {
            page = containerRepository.findByPoolIdAndDeletedIsFalse(query.getPoolId(), pageRequest);
        } else {
            page = containerRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public ContainerPageQuery getById(Long id) {
        Container entity = containerRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(ContainerCreateCmd cmd) {
        Container entity = assembler.toEntity(cmd);
        Container saved = containerRepository.save(entity);

        // 同步绑定池状态
        if (cmd.getPoolId() != null) {
            poolRepository.findByIdAndDeletedIsFalse(cmd.getPoolId()).ifPresent(pool -> {
                pool.markLoaded();
                poolRepository.save(pool);
            });
        }

        log.info("[Container] created, traceId={}, id={}, containerNo={}, poolId={}",
                null, saved.getId(), saved.getContainerNo(), saved.getPoolId());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ContainerUpdateCmd cmd) {
        Container entity = containerRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        if (entity.getStatus().isTerminal()) {
            throw new BusinessException("logistics.container_cannot_modify", "到港状态禁止修改");
        }
        assembler.copyUpdate(cmd, entity);
        containerRepository.save(entity);
        log.info("[Container] updated, traceId={}, id={}, status={}", null, id, entity.getStatus());
    }

    @Transactional
    public void delete(Long id) {
        Container entity = containerRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        if (entity.getStatus() != ContainerStatus.CREATED) {
            throw new BusinessException("logistics.container_cannot_delete", "仅 CREATED 状态允许删除");
        }
        entity.markDeleted();
        containerRepository.save(entity);
        log.info("[Container] deleted, traceId={}, id={}", null, id);
    }

    /**
     * 将 LogisticsPlan 加入货柜（CREATED 状态时可用）。
     */
    @Transactional
    public void addPlan(Long containerId, Long logisticsPlanId) {
        Container container = containerRepository.findByIdAndDeletedIsFalse(containerId)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        LogisticsPlan plan = logisticsPlanRepository.findByIdAndDeletedIsFalse(logisticsPlanId)
                .orElseThrow(() -> new BusinessException("logistics.plan_not_found", "调配计划不存在"));

        container.addPlan(plan.getCargoVolumeCbm(), plan.getCargoWeightKg());
        containerRepository.save(container);

        plan.setContainerId(containerId);
        if (plan.getContainerNo() == null) {
            plan.setContainerNo(container.getContainerNo());
        }
        logisticsPlanRepository.save(plan);

        log.info("[Container] plan added, traceId={}, containerId={}, planId={}, totalCbm={}",
                null, containerId, logisticsPlanId, container.getTotalCbm());
    }

    /**
     * 根据拼柜池自动创建货柜（PoolReadyToLoadEvent 触发）。
     */
    @Transactional
    public Long createFromPool(Long poolId) {
        ConsolidationPool pool = poolRepository.findByIdAndDeletedIsFalse(poolId)
                .orElseThrow(() -> new BusinessException("logistics.pool_not_found", "拼柜池不存在"));

        Container container = new Container();
        container.setPoolId(poolId);
        container.setContainerType(
                pool.getTotalCbm().compareTo(new java.math.BigDecimal("67")) > 0
                        ? com.manpou.allinone.logistics.domain.model.ContainerType.GP40
                        : com.manpou.allinone.logistics.domain.model.ContainerType.GP20);

        Container saved = containerRepository.save(container);
        log.info("[Container] created from pool, traceId={}, containerId={}, poolId={}",
                null, saved.getId(), poolId);
        return saved.getId();
    }
}
