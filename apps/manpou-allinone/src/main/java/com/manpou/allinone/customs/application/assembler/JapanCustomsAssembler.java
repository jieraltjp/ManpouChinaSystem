package com.manpou.allinone.customs.application.assembler;

import com.manpou.allinone.customs.application.dto.*;
import com.manpou.allinone.customs.domain.model.JapanCustomsRecord;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class JapanCustomsAssembler {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public JapanCustomsRecord toEntity(JapanCustomsCreateCmd cmd) {
        JapanCustomsRecord entity = new JapanCustomsRecord();
        entity.generateEntryNo();
        copyCreate(cmd, entity);
        return entity;
    }

    public void copyCreate(JapanCustomsCreateCmd cmd, JapanCustomsRecord entity) {
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getDomesticCustomsId() != null) entity.setDomesticCustomsId(cmd.getDomesticCustomsId());
        if (cmd.getLogisticsPlanId() != null) entity.setLogisticsPlanId(cmd.getLogisticsPlanId());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getArrivalDate() != null) entity.setArrivalDate(cmd.getArrivalDate());
        if (cmd.getCustomsBroker() != null) entity.setCustomsBroker(cmd.getCustomsBroker());
        if (cmd.getBrokerPhone() != null) entity.setBrokerPhone(cmd.getBrokerPhone());
        if (cmd.getBrokerContact() != null) entity.setBrokerContact(cmd.getBrokerContact());
        if (cmd.getArrivalPort() != null) entity.setArrivalPort(cmd.getArrivalPort());
        if (cmd.getDeclaredWeightKg() != null) entity.setDeclaredWeightKg(cmd.getDeclaredWeightKg());
        if (cmd.getDeclaredVolumeCbm() != null) entity.setDeclaredVolumeCbm(cmd.getDeclaredVolumeCbm());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyUpdate(JapanCustomsUpdateCmd cmd, JapanCustomsRecord entity) {
        if (cmd.getArrivalDate() != null) entity.setArrivalDate(cmd.getArrivalDate());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getCustomsBroker() != null) entity.setCustomsBroker(cmd.getCustomsBroker());
        if (cmd.getBrokerPhone() != null) entity.setBrokerPhone(cmd.getBrokerPhone());
        if (cmd.getBrokerContact() != null) entity.setBrokerContact(cmd.getBrokerContact());
        if (cmd.getArrivalPort() != null) entity.setArrivalPort(cmd.getArrivalPort());
        if (cmd.getDeclaredWeightKg() != null) entity.setDeclaredWeightKg(cmd.getDeclaredWeightKg());
        if (cmd.getDeclaredVolumeCbm() != null) entity.setDeclaredVolumeCbm(cmd.getDeclaredVolumeCbm());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public JapanCustomsPageQuery toDto(JapanCustomsRecord entity) {
        return JapanCustomsPageQuery.builder()
                .id(entity.getId())
                .customsEntryNo(entity.getCustomsEntryNo())
                .procurementId(entity.getProcurementId())
                .domesticCustomsId(entity.getDomesticCustomsId())
                .logisticsPlanId(entity.getLogisticsPlanId())
                .subProductCode(entity.getSubProductCode())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .arrivalDate(entity.getArrivalDate())
                .customsBroker(entity.getCustomsBroker())
                .brokerPhone(entity.getBrokerPhone())
                .brokerContact(entity.getBrokerContact())
                .importDutyPaid(entity.getImportDutyPaid())
                .consumptionTaxPaid(entity.getConsumptionTaxPaid())
                .clearanceDate(entity.getClearanceDate())
                .arrivalPort(entity.getArrivalPort())
                .declaredWeightKg(entity.getDeclaredWeightKg())
                .declaredVolumeCbm(entity.getDeclaredVolumeCbm())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime() != null ? entity.getCreateTime().format(DT_FMT) : null)
                .updateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(DT_FMT) : null)
                .build();
    }
}
