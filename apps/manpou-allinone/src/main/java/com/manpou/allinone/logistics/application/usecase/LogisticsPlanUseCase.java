package com.manpou.allinone.logistics.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.port.QcQueryPort;
import com.manpou.allinone.logistics.application.assembler.LogisticsPlanAssembler;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanUpdateCmd;
import com.manpou.allinone.logistics.domain.event.LogisticsPlanInTransitEvent;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
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
public class LogisticsPlanUseCase {

    private final LogisticsPlanRepository logisticsPlanRepository;
    private final LogisticsPlanAssembler logisticsPlanAssembler;
    private final QcQueryPort qcQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<LogisticsPlanPageQuery> pageQuery(LogisticsPlanQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<LogisticsPlan> page;
        if (query.getStatus() != null) {
            page = logisticsPlanRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else if (query.getPlanType() != null) {
            page = logisticsPlanRepository.findByPlanTypeAndDeletedIsFalse(query.getPlanType(), pageRequest);
        } else if (query.getQcRecordId() != null) {
            page = logisticsPlanRepository.findByQcRecordIdAndDeletedIsFalse(query.getQcRecordId(), pageRequest);
        } else if (query.getProcurementId() != null) {
            page = logisticsPlanRepository.findByProcurementIdAndDeletedIsFalse(query.getProcurementId(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = logisticsPlanRepository.findByProductCodeAndDeletedIsFalse(query.getProductCode(), pageRequest);
        } else if (query.getContainerNo() != null && !query.getContainerNo().isBlank()) {
            page = logisticsPlanRepository.findByContainerNoContainingAndDeletedIsFalse(query.getContainerNo(), pageRequest);
        } else {
            page = logisticsPlanRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(logisticsPlanAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public LogisticsPlanPageQuery getById(Long id) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        return logisticsPlanAssembler.toDto(entity);
    }

    @Transactional
    public Long create(LogisticsPlanCreateCmd cmd) {
        // v1.2.0: 校验 qcRecordId 存在且验货结果为 PASS，并 auto-fill cargo 尺寸
        if (cmd.getQcRecordId() != null) {
            var qcRecord = qcQueryPort.findById(cmd.getQcRecordId())
                    .orElseThrow(() -> new BusinessException("logistics.qc_record_not_found", "验货记录不存在"));
            if (qcRecord.getResult() != com.manpou.allinone.qc.domain.model.QcResult.PASS) {
                throw new BusinessException("logistics.qc_record_not_pass", "验货结果非 PASS，无法创建调配计划");
            }
            // auto-fill: QC 记录含实际装箱尺寸，用于订舱计算
            if (cmd.getCargoLengthCm() == null && qcRecord.getBoxLengthCm() != null) cmd.setCargoLengthCm(qcRecord.getBoxLengthCm());
            if (cmd.getCargoWidthCm() == null && qcRecord.getBoxWidthCm() != null) cmd.setCargoWidthCm(qcRecord.getBoxWidthCm());
            if (cmd.getCargoHeightCm() == null && qcRecord.getBoxHeightCm() != null) cmd.setCargoHeightCm(qcRecord.getBoxHeightCm());
            if (cmd.getCargoWeightKg() == null && qcRecord.getGrossWeight() != null) cmd.setCargoWeightKg(qcRecord.getGrossWeight());
            // V49: 从 QC record 自动带入净重/毛重
            if (cmd.getNetWeightKg() == null && qcRecord.getNetWeightPerUnit() != null) cmd.setNetWeightKg(qcRecord.getNetWeightPerUnit());
            if (cmd.getGrossWeightKg() == null && qcRecord.getGrossWeight() != null) cmd.setGrossWeightKg(qcRecord.getGrossWeight());
            // SPEC-B00 数量等式：quantity = passedCount（验货合格量），非采购量
            if (cmd.getQuantity() == null && qcRecord.getPassedCount() != null) cmd.setQuantity(qcRecord.getPassedCount());
        }
        LogisticsPlan entity = logisticsPlanAssembler.toEntity(cmd);
        entity.calculateVolume();
        LogisticsPlan saved = logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] created, traceId={}, id={}, planCode={}, planType={}, qcRecordId={}",
                null, saved.getId(), saved.getPlanCode(), saved.getPlanType(), saved.getQcRecordId());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, LogisticsPlanUpdateCmd cmd) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("logistics.cannot_modify_delivered", "调配计划已完成，禁止修改");
        }
        LogisticsStatus oldStatus = entity.getStatus();
        logisticsPlanAssembler.copyUpdate(cmd, entity);
        // EV-121: 推进至 IN_TRANSIT 时发布事件，触发国内报关自动创建
        if (cmd.getStatus() != null && cmd.getStatus() == LogisticsStatus.IN_TRANSIT
                && oldStatus != LogisticsStatus.IN_TRANSIT) {
            entity.updateStatus(cmd.getStatus());
            eventPublisher.publishEvent(new LogisticsPlanInTransitEvent(
                    entity.getId(),
                    entity.getPlanCode(),
                    entity.getProcurementId(),
                    entity.getFactoryId(),
                    entity.getProductCode(),
                    entity.getSubProductCode()
            ));
        }
        logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] updated, traceId={}, id={}, status={}", null, id, entity.getStatus());
    }

    @Transactional
    public void delete(Long id) {
        LogisticsPlan entity = logisticsPlanRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("logistics.not_found", "调配计划不存在"));
        if (entity.isTerminal()) {
            throw new BusinessException("logistics.cannot_delete_delivered", "已完成/运输中的调配计划禁止删除");
        }
        entity.markDeleted();
        logisticsPlanRepository.save(entity);
        log.info("[LogisticsPlan] deleted, traceId={}, id={}", null, id);
    }

    @Transactional
    public int batchUpdateCustomsClearanceNo(java.util.List<Long> ids, String customsClearanceNo) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("logistics.batch_empty", "请选择要修改的调配计划");
        }
        int count = 0;
        for (Long id : ids) {
            int updated = logisticsPlanRepository.findByIdAndDeletedIsFalse(id).map(entity -> {
                entity.setCustomsClearanceNo(customsClearanceNo);
                logisticsPlanRepository.save(entity);
                log.info("[LogisticsPlan] batch update customsClearanceNo, id={}, value={}", id, customsClearanceNo);
                return 1;
            }).orElse(0);
            count += updated;
        }
        return count;
    }
}
