package com.manpou.allinone.sales.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.sales.application.assembler.SalesRecordAssembler;
import com.manpou.allinone.sales.application.dto.*;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import com.manpou.allinone.sales.domain.model.SalesStatus;
import com.manpou.allinone.sales.domain.repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesRecordUseCase {

    private final SalesRecordRepository salesRecordRepository;
    private final SalesRecordAssembler assembler;

    @Transactional(readOnly = true)
    public Page<SalesRecordPageQuery> pageQuery(SalesRecordQuery query) {
        List<SalesRecord> all = salesRecordRepository.findByDeletedFalseOrderByCreateTimeDesc();
        List<SalesRecordPageQuery> filtered = all.stream()
                .filter(r -> query.getProductCode() == null || query.getProductCode().equals(r.getProductCode()))
                .filter(r -> query.getSalesChannel() == null || query.getSalesChannel().equals(r.getSalesChannel()))
                .filter(r -> query.getStatus() == null || query.getStatus().equals(r.getStatus().name()))
                .filter(r -> query.getProcurementId() == null || query.getProcurementId().equals(r.getProcurementId()))
                .map(assembler::toDto)
                .toList();
        int total = filtered.size();
        int page = query.getPage();
        int pageSize = Math.min(query.getPageSize(), 100);
        int from = page * pageSize;
        int to = Math.min(from + pageSize, total);
        List<SalesRecordPageQuery> paged = from >= total ? List.of() : filtered.subList(from, to);
        return new PageImpl<>(paged, PageRequest.of(page, pageSize), total);
    }

    @Transactional(readOnly = true)
    public SalesRecordPageQuery getById(Long id) {
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(SalesRecordCreateCmd cmd) {
        log.info("[SalesRecord] create, procurementId={}, productCode={}, salesChannel={}, initialStock={}", cmd.getProcurementId(), cmd.getProductCode(), cmd.getSalesChannel(), cmd.getInitialStock());
        SalesRecord entity = assembler.toEntity(cmd);
        SalesRecord saved = salesRecordRepository.save(entity);
        log.info("[SalesRecord] created, id={}, code={}", saved.getId(), saved.getRecordCode());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, SalesRecordUpdateCmd cmd) {
        log.info("[SalesRecord] update, id={}, sellingPriceJpy={}", id, cmd.getSellingPriceJpy());
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        if (entity.isTerminal()) {
            throw new BusinessException("sales.record.cannot_update_terminal", "已下架的记录禁止修改");
        }
        assembler.copyUpdate(cmd, entity);
        salesRecordRepository.save(entity);
        log.info("[SalesRecord] updated, id={}", id);
    }

    @Transactional
    public void updateStock(Long id, SalesRecordStockCmd cmd) {
        log.info("[SalesRecord] updateStock, id={}, sold={}, returned={}", id, cmd.getSold(), cmd.getReturned());
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        if (entity.isTerminal()) {
            throw new BusinessException("sales.record.cannot_update_terminal", "已下架的记录禁止修改库存");
        }
        entity.updateStock(cmd.getSold(), cmd.getReturned());
        entity.recalculateReturnRate();
        salesRecordRepository.save(entity);
        log.info("[SalesRecord] stock updated, id={}, sold={}, returned={}", id, cmd.getSold(), cmd.getReturned());
    }

    @Transactional
    public void discontinue(Long id) {
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        entity.discontinue();
        salesRecordRepository.save(entity);
        log.info("[SalesRecord] discontinued, id={}", id);
    }

    @Transactional
    public void relist(Long id) {
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        entity.relist();
        salesRecordRepository.save(entity);
        log.info("[SalesRecord] relisted, id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        SalesRecord entity = salesRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("SalesRecord", id));
        if (entity.isTerminal()) {
            throw new BusinessException("sales.record.cannot_delete_terminal", "已下架的记录禁止删除");
        }
        entity.markDeleted();
        salesRecordRepository.save(entity);
        log.info("[SalesRecord] deleted, id={}", id);
    }

    @Transactional(readOnly = true)
    public Page<SalesRecordPageQuery> getAlerts(SalesRecordQuery query) {
        List<String> alertStatuses = List.of(SalesStatus.LOW_STOCK.name(), SalesStatus.OUT_OF_STOCK.name());
        List<SalesRecord> all = salesRecordRepository.findByStatusInAndDeletedFalseOrderByCreateTimeDesc(alertStatuses);
        List<SalesRecordPageQuery> filtered = all.stream()
                .filter(r -> query.getProductCode() == null || query.getProductCode().equals(r.getProductCode()))
                .map(assembler::toDto)
                .toList();
        int total = filtered.size();
        int page = query.getPage();
        int pageSize = Math.min(query.getPageSize(), 100);
        int from = page * pageSize;
        int to = Math.min(from + pageSize, total);
        List<SalesRecordPageQuery> paged = from >= total ? List.of() : filtered.subList(from, to);
        return new PageImpl<>(paged, PageRequest.of(page, pageSize), total);
    }
}
