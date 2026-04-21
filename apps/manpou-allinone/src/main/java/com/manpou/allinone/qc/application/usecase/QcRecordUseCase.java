package com.manpou.allinone.qc.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.qc.application.assembler.QcRecordAssembler;
import com.manpou.allinone.qc.application.dto.QcRecordCreateCmd;
import com.manpou.allinone.qc.application.dto.QcRecordPageQuery;
import com.manpou.allinone.qc.application.dto.QcRecordQuery;
import com.manpou.allinone.qc.application.dto.QcRecordUpdateCmd;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
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
public class QcRecordUseCase {

    private final QcRecordRepository qcRecordRepository;
    private final QcRecordAssembler qcRecordAssembler;

    @Transactional(readOnly = true)
    public Page<QcRecordPageQuery> pageQuery(QcRecordQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<QcRecord> page;
        if (query.getResult() != null) {
            page = qcRecordRepository.findByResultAndIsDeletedFalse(query.getResult(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = qcRecordRepository.findByProductCodeAndIsDeletedFalse(query.getProductCode(), pageRequest);
        } else if (query.getProcurementId() != null) {
            page = qcRecordRepository.findByProcurementIdAndIsDeletedFalse(query.getProcurementId(), pageRequest);
        } else {
            page = qcRecordRepository.findAllByIsDeletedFalse(pageRequest);
        }
        return page.map(qcRecordAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public QcRecordPageQuery getById(Long id) {
        QcRecord entity = qcRecordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        return qcRecordAssembler.toDto(entity);
    }

    @Transactional
    public Long create(QcRecordCreateCmd cmd) {
        if (cmd.getInspectionCount() != null && cmd.getPassedCount() != null) {
            if (cmd.getInspectionCount() < cmd.getPassedCount()) {
                throw new BusinessException("qc.invalid_count", "合格数量不能大于检品数");
            }
        }
        QcRecord entity = qcRecordAssembler.toEntity(cmd);
        entity.calculateDefectiveCount();
        QcRecord saved = qcRecordRepository.save(entity);
        log.info("[QcRecord] created, traceId={}, id={}, qcCode={}, result={}",
                null, saved.getId(), saved.getQcCode(), saved.getResult());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, QcRecordUpdateCmd cmd) {
        QcRecord entity = qcRecordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("qc.cannot_modify_completed", "验货记录已完成，禁止修改");
        }
        if (cmd.getInspectionCount() != null && cmd.getPassedCount() != null) {
            if (cmd.getInspectionCount() < cmd.getPassedCount()) {
                throw new BusinessException("qc.invalid_count", "合格数量不能大于检品数");
            }
        }
        qcRecordAssembler.copyUpdate(cmd, entity);
        // 提交结果时自动推进状态
        if (cmd.getResult() != null && cmd.getStatus() == null) {
            entity.setStatus(QcStatus.COMPLETED);
        }
        qcRecordRepository.save(entity);
        log.info("[QcRecord] updated, traceId={}, id={}, result={}", null, id, entity.getResult());
    }

    @Transactional
    public void delete(Long id) {
        QcRecord entity = qcRecordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        if (entity.getStatus() != QcStatus.PENDING) {
            throw new BusinessException("qc.only_pending_delete", "仅待验货状态可删除");
        }
        entity.markDeleted();
        qcRecordRepository.save(entity);
        log.info("[QcRecord] deleted, traceId={}, id={}", null, id);
    }
}
