package com.manpou.allinone.legacyprocurement.application.usecase;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import com.manpou.allinone.legacyprocurement.application.assembler.LegacyProcurementAssembler;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementPageVO;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementQuery;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementStatsDTO;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementCreateCmd;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementUpdateCmd;
import com.manpou.allinone.legacyprocurement.domain.model.LegacyProcurement;
import com.manpou.allinone.legacyprocurement.domain.repository.LegacyProcurementRepository;
import com.manpou.allinone.legacyprocurement.infrastructure.persistence.jpa.JpaLegacyProcurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyProcurementUseCase {

    private final LegacyProcurementRepository repository;
    private final JpaLegacyProcurementRepository jpaRepository;
    private final LegacyProcurementAssembler assembler;

    @Transactional(readOnly = true)
    public Page<LegacyProcurementPageVO> pageQuery(LegacyProcurementQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "updatetime")
        );

        String code = query.getCode();
        String orderGroup = query.getOrderGroup();
        String itemName = query.getItemName();
        String updater = query.getUpdater();

        Page<LegacyProcurement> page;
        if (code != null && !code.isBlank()) {
            page = repository.findByCodeContainingExcludeDeleted(code, pageRequest);
        } else if (orderGroup != null && !orderGroup.isBlank()) {
            page = repository.findByOrderGroupContainingExcludeDeleted(orderGroup, pageRequest);
        } else if (itemName != null && !itemName.isBlank()) {
            page = repository.findByItemNameContainingExcludeDeleted(itemName, pageRequest);
        } else if (updater != null && !updater.isBlank()) {
            page = repository.findByUpdaterContainingExcludeDeleted(updater, pageRequest);
        } else {
            page = repository.findAllExcludeDeleted(pageRequest);
        }
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public LegacyProcurementPageVO getById(Integer id) {
        LegacyProcurement entity = repository.findByIdExcludeDeleted(id)
                .orElseThrow(() -> BusinessException.notFound("LegacyProcurement", id));
        return assembler.toDto(entity);
    }

    @Transactional
    @AuditLog(module = "legacy_procurement", action = "CREATE", resourceType = "legacy_procurement",
            resourceId = "0", resourceCode = "")
    public LegacyProcurementPageVO create(LegacyProcurementCreateCmd cmd) {
        LegacyProcurement entity = assembler.toEntity(cmd);
        entity.setDeleted(false);
        entity.setUpdater(JwtContextHolder.getUsername());
        entity.setUpdatetime(LocalDateTime.now());
        LegacyProcurement saved = repository.save(entity);
        log.info("LegacyProcurement 新建: id={}", saved.getLegacyId());
        return assembler.toDto(saved);
    }

    @Transactional
    @AuditLog(module = "legacy_procurement", action = "UPDATE", resourceType = "legacy_procurement",
            resourceId = "#id", resourceCode = "")
    public LegacyProcurementPageVO update(Integer id, LegacyProcurementUpdateCmd cmd) {
        LegacyProcurement entity = repository.findByIdExcludeDeleted(id)
                .orElseThrow(() -> BusinessException.notFound("LegacyProcurement", id));

        if (cmd.getOrderGroup() != null) entity.setOrderGroup(cmd.getOrderGroup());
        if (cmd.getOrderCount() != null) entity.setOrderCount(cmd.getOrderCount());
        if (cmd.getInspectCount() != null) entity.setInspectCount(cmd.getInspectCount());
        if (cmd.getYoyakuHasoubi() != null) entity.setYoyakuHasoubi(cmd.getYoyakuHasoubi());
        if (cmd.getArrivalDepo() != null) entity.setArrivalDepo(cmd.getArrivalDepo());
        if (cmd.getDeparture() != null) entity.setDeparture(cmd.getDeparture());
        if (cmd.getArrival() != null) entity.setArrival(cmd.getArrival());
        if (cmd.getUnitCh() != null) entity.setUnitCh(cmd.getUnitCh());
        if (cmd.getTotalCh() != null) entity.setTotalCh(cmd.getTotalCh());
        if (cmd.getUnitJp() != null) entity.setUnitJp(cmd.getUnitJp());
        if (cmd.getTotalJp() != null) entity.setTotalJp(cmd.getTotalJp());
        if (cmd.getRate() != null) entity.setRate(cmd.getRate());
        if (cmd.getFbaStock() != null) entity.setFbaStock(cmd.getFbaStock());
        if (cmd.getHoukoku() != null) entity.setHoukoku(cmd.getHoukoku());
        if (cmd.getKaitsuke() != null) entity.setKaitsuke(cmd.getKaitsuke());
        if (cmd.getHyoten() != null) entity.setHyoten(cmd.getHyoten());
        if (cmd.getKanpu() != null) entity.setKanpu(cmd.getKanpu());
        if (cmd.getContainer() != null) entity.setContainer(cmd.getContainer());
        if (cmd.getBoxNum() != null) entity.setBoxNum(cmd.getBoxNum());
        if (cmd.getBoxCount() != null) entity.setBoxCount(cmd.getBoxCount());
        if (cmd.getKg() != null) entity.setKg(cmd.getKg());
        if (cmd.getOneM3() != null) entity.setOneM3(cmd.getOneM3());
        if (cmd.getAllM3() != null) entity.setAllM3(cmd.getAllM3());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getMaterialCh() != null) entity.setMaterialCh(cmd.getMaterialCh());
        if (cmd.getHeight() != null) entity.setHeight(cmd.getHeight());
        if (cmd.getWidth() != null) entity.setWidth(cmd.getWidth());
        if (cmd.getDepth() != null) entity.setDepth(cmd.getDepth());
        if (cmd.getNote() != null) entity.setNote(cmd.getNote());
        if (cmd.getReceive() != null) entity.setReceive(cmd.getReceive());

        entity.setUpdatetime(LocalDateTime.now());
        entity.setUpdater(JwtContextHolder.getUsername());

        LegacyProcurement saved = repository.save(entity);
        log.info("LegacyProcurement 更新: id={}", id);
        return assembler.toDto(saved);
    }

    @Transactional
    @AuditLog(module = "legacy_procurement", action = "DELETE", resourceType = "legacy_procurement",
            resourceId = "#id", resourceCode = "")
    public void softDelete(Integer id) {
        if (!repository.findByIdExcludeDeleted(id).isPresent()) {
            throw BusinessException.notFound("LegacyProcurement", id);
        }
        repository.softDelete(id);
        log.info("LegacyProcurement 软删除: id={}", id);
    }

    @Transactional(readOnly = true)
    public LegacyProcurementStatsDTO stats() {
        return LegacyProcurementStatsDTO.builder()
                .total(repository.count())
                .withContainer(jpaRepository.countByContainerIsNotNullAndDeletedFalse())
                .withImg(jpaRepository.countByImgIsNotNullAndImgNotAndDeletedFalse(""))
                .build();
    }
}