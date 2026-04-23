package com.manpou.allinone.finance.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.finance.application.assembler.TaxRefundAssembler;
import com.manpou.allinone.finance.application.dto.*;
import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import com.manpou.allinone.finance.domain.repository.TaxRefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxRefundUseCase {

    private final TaxRefundRepository taxRefundRepository;
    private final TaxRefundAssembler assembler;

    @Transactional(readOnly = true)
    public Page<TaxRefundPageQuery> pageQuery(TaxRefundQuery query) {
        List<TaxRefundRecord> all = taxRefundRepository.findByIsDeletedFalseOrderByCreateTimeDesc();
        List<TaxRefundPageQuery> filtered = all.stream()
                .filter(r -> query.getProcurementId() == null || query.getProcurementId().equals(r.getProcurementId()))
                .filter(r -> query.getStatus() == null || query.getStatus().equals(r.getStatus().name()))
                .map(assembler::toDto)
                .toList();
        int total = filtered.size();
        int page = query.getPage();
        int pageSize = Math.min(query.getPageSize(), 100);
        int from = page * pageSize;
        int to = Math.min(from + pageSize, total);
        List<TaxRefundPageQuery> paged = from >= total ? List.of() : filtered.subList(from, to);
        return new org.springframework.data.domain.PageImpl<>(paged,
                PageRequest.of(page, pageSize), total);
    }

    @Transactional(readOnly = true)
    public TaxRefundPageQuery getById(Long id) {
        TaxRefundRecord entity = taxRefundRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("TaxRefund", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public Long create(TaxRefundCreateCmd cmd) {
        log.info("[TaxRefund] create, procurementId={}, japanCustomsId={}, billingType={}", cmd.getProcurementId(), cmd.getJapanCustomsId(), cmd.getBillingType());
        TaxRefundRecord entity = assembler.toEntity(cmd);
        TaxRefundRecord saved = taxRefundRepository.save(entity);
        log.info("[TaxRefund] created, id={}, code={}", saved.getId(), saved.getRefundCode());
        return saved.getId();
    }

    @Transactional
    public void complete(Long id, TaxRefundCompleteCmd cmd) {
        log.info("[TaxRefund] complete, id={}, actualRefundRmb={}, refundDate={}, refundBank={}", id, cmd.getActualRefundRmb(), cmd.getRefundDate(), cmd.getRefundBank());
        TaxRefundRecord entity = taxRefundRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("TaxRefund", id));
        entity.complete(cmd.getActualRefundRmb(), cmd.getRefundDate(), cmd.getRefundBank());
        taxRefundRepository.save(entity);
        log.info("[TaxRefund] completed, id={}", id);
    }

    @Transactional
    public void markNoRefund(Long id) {
        TaxRefundRecord entity = taxRefundRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("TaxRefund", id));
        entity.markNoRefund();
        taxRefundRepository.save(entity);
        log.info("[TaxRefund] marked no-refund, id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        TaxRefundRecord entity = taxRefundRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("TaxRefund", id));
        if (entity.isTerminal()) {
            throw new BusinessException("tax.refund.cannot_delete_terminal", "已完成/不退税的记录禁止删除");
        }
        entity.markDeleted();
        taxRefundRepository.save(entity);
        log.info("[TaxRefund] deleted, id={}", id);
    }
}
