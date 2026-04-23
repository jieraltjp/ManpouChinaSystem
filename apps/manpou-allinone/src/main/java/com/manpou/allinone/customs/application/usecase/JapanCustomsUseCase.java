package com.manpou.allinone.customs.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.customs.application.assembler.JapanCustomsAssembler;
import com.manpou.allinone.customs.application.dto.*;
import com.manpou.allinone.customs.domain.event.JapanCustomsClearedEvent;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import com.manpou.allinone.customs.domain.repository.JapanCustomsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JapanCustomsUseCase {

    private final JapanCustomsRepository japanCustomsRepository;
    private final JapanCustomsAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<JapanCustomsPageQuery> pageQuery(JapanCustomsQuery query) {
        List<JapanCustomsRecord> all = japanCustomsRepository.findByDeletedFalseOrderByCreateTimeDesc();
        List<JapanCustomsPageQuery> filtered = all.stream()
                .filter(r -> query.getProcurementId() == null || query.getProcurementId().equals(r.getProcurementId()))
                .filter(r -> query.getDomesticCustomsId() == null || query.getDomesticCustomsId().equals(r.getDomesticCustomsId()))
                .filter(r -> query.getStatus() == null || query.getStatus().equals(r.getStatus().name()))
                .map(assembler::toDto)
                .toList();
        int total = filtered.size();
        int page = query.getPage();
        int pageSize = Math.min(query.getPageSize(), 100);
        int from = page * pageSize;
        int to = Math.min(from + pageSize, total);
        List<JapanCustomsPageQuery> paged = from >= total ? List.of() : filtered.subList(from, to);
        return new org.springframework.data.domain.PageImpl<>(paged,
                PageRequest.of(page, pageSize), total);
    }

    @Transactional(readOnly = true)
    public JapanCustomsPageQuery getById(Long id) {
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(JapanCustomsCreateCmd cmd) {
        log.info("[JapanCustoms] create, procurementId={}, domesticCustomsId={}, arrivalDate={}", cmd.getProcurementId(), cmd.getDomesticCustomsId(), cmd.getArrivalDate());
        JapanCustomsRecord entity = assembler.toEntity(cmd);
        JapanCustomsRecord saved = japanCustomsRepository.save(entity);
        log.info("[JapanCustoms] created, id={}, entryNo={}", saved.getId(), saved.getCustomsEntryNo());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, JapanCustomsUpdateCmd cmd) {
        log.info("[JapanCustoms] update, id={}, customsBroker={}, arrivalPort={}", id, cmd.getCustomsBroker(), cmd.getArrivalPort());
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        if (entity.isTerminal()) {
            throw new BusinessException("japan.customs.cannot_modify_terminal", "已完成/失败的清关记录禁止修改");
        }
        assembler.copyUpdate(cmd, entity);
        japanCustomsRepository.save(entity);
        log.info("[JapanCustoms] updated, id={}", id);
    }

    @Transactional
    public void startClearance(Long id) {
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        entity.startClearance();
        japanCustomsRepository.save(entity);
        log.info("[JapanCustoms] started, id={}", id);
    }

    @Transactional
    public void complete(Long id, JapanCustomsCompleteCmd cmd) {
        log.info("[JapanCustoms] complete, id={}, importDutyPaid={}, consumptionTaxPaid={}, clearanceDate={}", id, cmd.getImportDutyPaid(), cmd.getConsumptionTaxPaid(), cmd.getClearanceDate());
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        entity.complete(cmd.getImportDutyPaid(), cmd.getConsumptionTaxPaid(), cmd.getClearanceDate());
        japanCustomsRepository.save(entity);
        // 发布清关完成事件，触发 SalesRecord 自动创建（步骤8）
        eventPublisher.publishEvent(new JapanCustomsClearedEvent(this, id, entity.getProcurementId()));
        log.info("[JapanCustoms] completed, id={}", id);
    }

    @Transactional
    public void fail(Long id, JapanCustomsFailCmd cmd) {
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        entity.fail(cmd.getReason());
        japanCustomsRepository.save(entity);
        log.info("[JapanCustoms] failed, id={}, reason={}", id, cmd.getReason());
    }

    @Transactional
    public void delete(Long id) {
        JapanCustomsRecord entity = japanCustomsRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("JapanCustoms", id));
        if (entity.isTerminal()) {
            throw new BusinessException("japan.customs.cannot_delete_terminal", "已完成/失败的清关记录禁止删除");
        }
        entity.markDeleted();
        japanCustomsRepository.save(entity);
        log.info("[JapanCustoms] deleted, id={}", id);
    }
}
