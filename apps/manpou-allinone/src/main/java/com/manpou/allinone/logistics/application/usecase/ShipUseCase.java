package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.assembler.ShipAssembler;
import com.manpou.allinone.logistics.application.dto.ship.ShipCreateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipQuery;
import com.manpou.allinone.logistics.application.dto.ship.ShipUpdateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipVO;
import com.manpou.allinone.logistics.domain.model.Ship;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import com.manpou.allinone.logistics.domain.repository.ShipRepository;
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
public class ShipUseCase {

    private final ShipRepository shipRepository;
    private final ContainerRepository containerRepository;
    private final ShipAssembler assembler;

    @Transactional(readOnly = true)
    public Page<ShipVO> pageQuery(ShipQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Ship> page;
        if (query.getShipName() != null && !query.getShipName().isBlank()) {
            page = shipRepository.findByShipNameContainingAndDeletedIsFalse(query.getShipName().trim(), pageRequest);
        } else if (query.getShipNumber() != null && !query.getShipNumber().isBlank()) {
            page = shipRepository.findByShipNumberContainingAndDeletedIsFalse(query.getShipNumber().trim(), pageRequest);
        } else if (query.getArrivalPort() != null && !query.getArrivalPort().isBlank()) {
            page = shipRepository.findByArrivalPortContainingAndDeletedIsFalse(query.getArrivalPort().trim(), pageRequest);
        } else {
            page = shipRepository.findAllByDeletedIsFalse(pageRequest);
        }

        return page.map(entity -> assembler.toVo(entity, containerRepository.countByShipIdAndDeletedIsFalse(entity.getId())));
    }

    @Transactional(readOnly = true)
    public ShipVO getById(Long id) {
        Ship entity = shipRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.ship_not_found", "船只不存在"));
        return assembler.toVo(entity, containerRepository.countByShipIdAndDeletedIsFalse(id));
    }

    @Transactional
    public Long create(ShipCreateCmd cmd) {
        // 船号唯一性校验
        if (shipRepository.findByShipNumberAndDeletedIsFalse(cmd.getShipNumber()).isPresent()) {
            throw new BusinessException("logistics.ship_number_exists", "船号已存在: " + cmd.getShipNumber());
        }

        Ship entity = assembler.toEntity(cmd);
        Ship saved = shipRepository.save(entity);
        log.info("[Ship] created, id={}, shipName={}, shipNumber={}",
                saved.getId(), saved.getShipName(), saved.getShipNumber());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ShipUpdateCmd cmd) {
        Ship entity = shipRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.ship_not_found", "船只不存在"));
        assembler.copyUpdate(cmd, entity);
        shipRepository.save(entity);
        log.info("[Ship] updated, id={}, shipName={}", id, entity.getShipName());
    }

    @Transactional
    public void delete(Long id) {
        Ship entity = shipRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.ship_not_found", "船只不存在"));

        // 确认无关联货柜
        long count = containerRepository.countByShipIdAndDeletedIsFalse(id);
        if (count > 0) {
            throw new BusinessException("logistics.ship_has_containers",
                    String.format("该船只下有 %d 个货柜，请先解除关联", count));
        }

        entity.markDeleted();
        shipRepository.save(entity);
        log.info("[Ship] deleted, id={}", id);
    }
}
