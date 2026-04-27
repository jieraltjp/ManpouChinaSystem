package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.common.port.FactoryQueryPort;
import com.manpou.allinone.common.port.QcQueryPort;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanUpdateCmd;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LogisticsPlanAssembler {

    private final FactoryQueryPort factoryQueryPort;
    private final QcQueryPort qcQueryPort;

    public LogisticsPlanAssembler(FactoryQueryPort factoryQueryPort, QcQueryPort qcQueryPort) {
        this.factoryQueryPort = factoryQueryPort;
        this.qcQueryPort = qcQueryPort;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generatePlanCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("L-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public LogisticsPlanPageQuery toDto(LogisticsPlan entity) {
        String factoryName = null;
        if (entity.getFactoryId() != null) {
            factoryName = factoryQueryPort
                    .findByIdAndDeletedIsFalse(entity.getFactoryId())
                    .map(f -> f.getFactoryName())
                    .orElse(null);
        }
        String qcCode = null;
        if (entity.getQcRecordId() != null) {
            qcCode = qcQueryPort.findById(entity.getQcRecordId())
                    .map(qc -> qc.getQcCode())
                    .orElse(null);
        }
        return LogisticsPlanPageQuery.builder()
                .id(entity.getId())
                .planCode(entity.getPlanCode())
                .qcRecordId(entity.getQcRecordId())
                .qcCode(qcCode)
                .procurementId(entity.getProcurementId())
                .factoryId(entity.getFactoryId())
                .factoryName(factoryName)
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .planType(entity.getPlanType())
                .status(entity.getStatus())
                .cargoLengthCm(entity.getCargoLengthCm())
                .cargoWidthCm(entity.getCargoWidthCm())
                .cargoHeightCm(entity.getCargoHeightCm())
                .cargoWeightKg(entity.getCargoWeightKg())
                .cargoVolumeCbm(entity.getCargoVolumeCbm())
                .quantity(entity.getQuantity())
                .requiresQc(entity.getRequiresQc())
                .containerId(entity.getContainerId())
                .poolId(entity.getPoolId())
                .estimatedShipDate(entity.getEstimatedShipDate())
                .actualShipDate(entity.getActualShipDate())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public LogisticsPlan toEntity(LogisticsPlanCreateCmd cmd) {
        LogisticsPlan entity = new LogisticsPlan();
        entity.setPlanCode(generatePlanCode());
        entity.setProductCode(cmd.getProductCode());
        copyCreate(cmd, entity);
        return entity;
    }

    public void copyCreate(LogisticsPlanCreateCmd cmd, LogisticsPlan entity) {
        if (cmd.getQcRecordId() != null) entity.setQcRecordId(cmd.getQcRecordId());
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getFactoryId() != null) entity.setFactoryId(cmd.getFactoryId());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getPlanType() != null) entity.setPlanType(cmd.getPlanType());
        if (cmd.getCargoLengthCm() != null) entity.setCargoLengthCm(cmd.getCargoLengthCm());
        if (cmd.getCargoWidthCm() != null) entity.setCargoWidthCm(cmd.getCargoWidthCm());
        if (cmd.getCargoHeightCm() != null) entity.setCargoHeightCm(cmd.getCargoHeightCm());
        if (cmd.getCargoWeightKg() != null) entity.setCargoWeightKg(cmd.getCargoWeightKg());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        if (cmd.getEstimatedShipDate() != null) entity.setEstimatedShipDate(cmd.getEstimatedShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyUpdate(LogisticsPlanUpdateCmd cmd, LogisticsPlan entity) {
        if (cmd.getPlanType() != null) entity.setPlanType(cmd.getPlanType());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
        if (cmd.getCargoLengthCm() != null) entity.setCargoLengthCm(cmd.getCargoLengthCm());
        if (cmd.getCargoWidthCm() != null) entity.setCargoWidthCm(cmd.getCargoWidthCm());
        if (cmd.getCargoHeightCm() != null) entity.setCargoHeightCm(cmd.getCargoHeightCm());
        if (cmd.getCargoWeightKg() != null) entity.setCargoWeightKg(cmd.getCargoWeightKg());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        if (cmd.getContainerId() != null) entity.setContainerId(cmd.getContainerId());
        if (cmd.getPoolId() != null) entity.setPoolId(cmd.getPoolId());
        if (cmd.getEstimatedShipDate() != null) entity.setEstimatedShipDate(cmd.getEstimatedShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getCargoLengthCm() != null || cmd.getCargoWidthCm() != null || cmd.getCargoHeightCm() != null) {
            entity.calculateVolume();
        }
    }
}
