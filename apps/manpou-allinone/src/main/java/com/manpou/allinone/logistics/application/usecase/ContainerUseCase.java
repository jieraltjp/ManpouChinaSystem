package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.assembler.ContainerAssembler;
import com.manpou.allinone.logistics.application.dto.AssignShipCmd;
import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.domain.model.Container;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.Ship;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import com.manpou.allinone.logistics.domain.repository.ShipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerUseCase {

    private final ContainerRepository containerRepository;
    private final ShipRepository shipRepository;
    private final ContainerAssembler assembler;

    @Transactional(readOnly = true)
    public Page<ContainerPageQuery> pageQuery(ContainerQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100));

        // ===== list7 筛选（SPEC-B14）=====
        if (Boolean.TRUE.equals(query.getShowFlag())) {
            Page<Object[]> page = containerRepository.findAllWithShipByShowFlag(true, pageRequest);
            return page.map(assembler::toDtoFromArray);
        } else if (query.getLegacyStatus() != null && !query.getLegacyStatus().isBlank()) {
            Page<Object[]> page = containerRepository.findAllWithShipByLegacyStatus(query.getLegacyStatus(), pageRequest);
            return page.map(assembler::toDtoFromArray);
        } else if (query.getCabinetNo() != null && !query.getCabinetNo().isBlank()) {
            Page<Object[]> page = containerRepository.findAllWithShipByCabinetNoContaining(query.getCabinetNo(), pageRequest);
            return page.map(assembler::toDtoFromArray);
        } else if (query.getShipId() != null) {
            if (Boolean.TRUE.equals(query.getShowFlag())) {
                Page<Object[]> page = containerRepository.findAllWithShipByShipIdAndShowFlag(query.getShipId(), true, pageRequest);
                return page.map(assembler::toDtoFromArray);
            }
            Page<Object[]> page = containerRepository.findAllWithShipByShipId(query.getShipId(), pageRequest);
            return page.map(assembler::toDtoFromArray);
        } else if (query.getStatus() != null) {
            Page<Object[]> page = containerRepository.findAllWithShipByStatus(query.getStatus(), pageRequest);
            return page.map(assembler::toDtoFromArray);
        } else {
            // 默认：JOIN ship 获取 shipName / shipNumber
            Page<Object[]> page = containerRepository.findAllWithShip(pageRequest);
            return page.map(assembler::toDtoFromArray);
        }
    }

    @Transactional(readOnly = true)
    public ContainerPageQuery getById(Long id) {
        Object[] row = containerRepository.findOneWithShip(id)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        return assembler.toDtoFromArray(row);
    }

    @Transactional
    public Long create(ContainerCreateCmd cmd) {
        Container entity = assembler.toEntity(cmd);
        Container saved = containerRepository.save(entity);
        log.info("[Container] created, id={}, containerNo={}", saved.getId(), saved.getContainerNo());
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
        log.info("[Container] updated, id={}, status={}", id, entity.getStatus());
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
        log.info("[Container] deleted, id={}", id);
    }

    @Transactional
    public void assignShip(Long containerId, AssignShipCmd cmd) {
        Container container = containerRepository.findByIdAndDeletedIsFalse(containerId)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        Ship ship = shipRepository.findByIdAndDeletedIsFalse(cmd.getShipId())
                .orElseThrow(() -> new BusinessException("logistics.ship_not_found", "船只不存在"));

        container.assignShip(cmd.getShipId(), cmd.getLoadDate());
        containerRepository.save(container);
        log.info("[Container] ship assigned, containerId={}, shipId={}, shipName={}, status={}",
                containerId, cmd.getShipId(), ship.getShipName(), container.getStatus());
    }

    @Transactional
    public void unassignShip(Long containerId) {
        Container container = containerRepository.findByIdAndDeletedIsFalse(containerId)
                .orElseThrow(() -> new BusinessException("logistics.container_not_found", "货柜不存在"));
        container.unassignShip();
        containerRepository.save(container);
        log.info("[Container] ship unassigned, containerId={}, status={}", containerId, container.getStatus());
    }
}
