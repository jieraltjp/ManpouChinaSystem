package com.manpou.allinone.qc.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.port.FactoryQueryPort;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.qc.application.assembler.QcRecordAssembler;
import com.manpou.allinone.qc.application.dto.QcRecordCreateCmd;
import com.manpou.allinone.qc.application.dto.QcRecordPageQuery;
import com.manpou.allinone.qc.application.dto.QcRecordQuery;
import com.manpou.allinone.qc.application.dto.QcRecordUpdateCmd;
import com.manpou.allinone.qc.domain.event.QcRecordCompletedEvent;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
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
public class QcRecordUseCase {

    private final QcRecordRepository qcRecordRepository;
    private final QcRecordAssembler qcRecordAssembler;
    private final ProcurementRepository procurementRepository;
    private final FactoryQueryPort factoryQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<QcRecordPageQuery> pageQuery(QcRecordQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<QcRecord> page;
        if (query.getShipmentBatchId() != null) {
            page = qcRecordRepository.findByShipmentBatchIdAndDeletedIsFalse(query.getShipmentBatchId(), pageRequest);
        } else if (query.getResult() != null) {
            page = qcRecordRepository.findByResultAndDeletedIsFalse(query.getResult(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = qcRecordRepository.findByProductCodeAndDeletedIsFalse(query.getProductCode(), pageRequest);
        } else if (query.getProcurementId() != null) {
            page = qcRecordRepository.findByProcurementIdAndDeletedIsFalse(query.getProcurementId(), pageRequest);
        } else {
            page = qcRecordRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(qcRecordAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public QcRecordPageQuery getById(Long id) {
        QcRecord entity = qcRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        return qcRecordAssembler.toDto(entity);
    }

    @Transactional
    public Long create(QcRecordCreateCmd cmd) {
        log.info("[QcRecord] create called, shipmentBatchId={}, productCode={}", cmd.getShipmentBatchId(), cmd.getProductCode());
        // V43: 新建验货必须关联出货批次
        if (cmd.getShipmentBatchId() == null) {
            throw new BusinessException("qc.validation.shipmentBatchRequired", "关联出货批次不能为空");
        }
        if (cmd.getInspectionCount() != null && cmd.getPassedCount() != null) {
            if (cmd.getInspectionCount() < cmd.getPassedCount()) {
                throw new BusinessException("qc.invalid_count", "合格数量不能大于检品数");
            }
        }
        // K-03: 自动代入 sellerName ← Factory.factoryName（当 sellerName 未填时）
        if (cmd.getSellerName() == null && cmd.getProcurementId() != null) {
            procurementRepository.findByIdAndDeletedIsFalse(cmd.getProcurementId())
                    .ifPresent(procurement -> {
                        if (procurement.getFactoryId() != null) {
                            factoryQueryPort.findByIdAndDeletedIsFalse(procurement.getFactoryId())
                                    .map(f -> f.getFactoryName())
                                    .filter(name -> !name.isBlank())
                                    .ifPresent(cmd::setSellerName);
                        }
                    });
        }
        QcRecord entity = qcRecordAssembler.toEntity(cmd);
        entity.calculateDefectiveCount();
        log.info("[QcRecord] about to save entity, qcCode={}, result={}", entity.getQcCode(), entity.getResult());
        QcRecord saved = qcRecordRepository.save(entity);
        log.info("[QcRecord] after save, id={}, qcCode={}", saved.getId(), saved.getQcCode());
        qcRecordRepository.flush();
        log.info("[QcRecord] after flush, id={}", saved.getId());
        log.info("[QcRecord] created, traceId={}, id={}, qcCode={}, result={}",
                null, saved.getId(), saved.getQcCode(), saved.getResult());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, QcRecordUpdateCmd cmd) {
        QcRecord entity = qcRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("qc.cannot_modify_completed", "验货记录已完成，禁止修改");
        }
        if (cmd.getInspectionCount() != null && cmd.getPassedCount() != null) {
            if (cmd.getInspectionCount() < cmd.getPassedCount()) {
                throw new BusinessException("qc.invalid_count", "合格数量不能大于检品数");
            }
        }
        boolean isCompleting = cmd.getResult() != null && cmd.getStatus() == null;
        qcRecordAssembler.copyUpdate(cmd, entity);
        if (isCompleting) {
            // EV-113: PASS 才推进至 COMPLETED；FAIL 直接结束，不触发后续采购链
            if (entity.getResult() == QcResult.PASS) {
                entity.updateStatus(QcStatus.COMPLETED);
                eventPublisher.publishEvent(new QcRecordCompletedEvent(
                        entity.getId(),
                        entity.getQcCode(),
                        entity.getProcurementId(),
                        entity.getShipmentBatchId(),
                        entity.getProductCode(),
                        entity.getSubProductCode()
                ));
            } else {
                entity.updateStatus(QcStatus.RETURN_REQUESTED);
            }
        }
        qcRecordRepository.save(entity);
        log.info("[QcRecord] updated, traceId={}, id={}, result={}", null, id, entity.getResult());
    }

    @Transactional
    public void delete(Long id) {
        QcRecord entity = qcRecordRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("qc.not_found", "验货记录不存在"));
        if (entity.getStatus() != QcStatus.PENDING) {
            throw new BusinessException("qc.only_pending_delete", "仅待验货状态可删除");
        }
        entity.markDeleted();
        qcRecordRepository.save(entity);
        log.info("[QcRecord] deleted, traceId={}, id={}", null, id);
    }
}
