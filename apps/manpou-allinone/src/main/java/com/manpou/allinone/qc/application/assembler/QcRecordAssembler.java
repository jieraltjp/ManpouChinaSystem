package com.manpou.allinone.qc.application.assembler;

import com.manpou.allinone.qc.application.dto.QcRecordCreateCmd;
import com.manpou.allinone.qc.application.dto.QcRecordPageQuery;
import com.manpou.allinone.qc.application.dto.QcRecordUpdateCmd;
import com.manpou.allinone.qc.domain.model.QcRecord;
import com.manpou.allinone.qc.domain.model.QcStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class QcRecordAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateQcCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("Q-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public QcRecordPageQuery toDto(QcRecord entity) {
        return QcRecordPageQuery.builder()
                .id(entity.getId())
                .qcCode(entity.getQcCode())
                .procurementId(entity.getProcurementId())
                .sellerName(entity.getSellerName())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .qcUserId(entity.getQcUserId())
                .qcType(entity.getQcType())
                .qcDate(entity.getQcDate())
                .result(entity.getResult())
                .status(entity.getStatus())
                .inspectionCount(entity.getInspectionCount())
                .passedCount(entity.getPassedCount())
                .defectiveCount(entity.getDefectiveCount())
                .boxCount(entity.getBoxCount())
                .boxLengthCm(entity.getBoxLengthCm())
                .boxWidthCm(entity.getBoxWidthCm())
                .boxHeightCm(entity.getBoxHeightCm())
                .netWeightPerUnit(entity.getNetWeightPerUnit())
                .grossWeight(entity.getGrossWeight())
                .taxInclusivePrice(entity.getTaxInclusivePrice())
                .material(entity.getMaterial())
                .taxRefund(entity.getTaxRefund())
                .qcStandard(entity.getQcStandard())
                .remarks(entity.getRemarks())
                .images(entity.getImages())
                .destination(entity.getDestination())
                .quantity(entity.getQuantity())
                .orderDate(entity.getOrderDate())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public QcRecord toEntity(QcRecordCreateCmd cmd) {
        QcRecord entity = new QcRecord();
        entity.setQcCode(generateQcCode());
        entity.setProcurementId(cmd.getProcurementId());
        entity.setProductCode(cmd.getProductCode());
        copyCreate(cmd, entity);
        entity.calculateDefectiveCount();
        return entity;
    }

    public void copyCreate(QcRecordCreateCmd cmd, QcRecord entity) {
        if (cmd.getSellerName() != null) entity.setSellerName(cmd.getSellerName());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getQcUserId() != null) entity.setQcUserId(cmd.getQcUserId());
        if (cmd.getQcType() != null) entity.setQcType(cmd.getQcType());
        if (cmd.getQcDate() != null) entity.setQcDate(cmd.getQcDate());
        if (cmd.getResult() != null) entity.setResult(cmd.getResult());
        if (cmd.getInspectionCount() != null) entity.setInspectionCount(cmd.getInspectionCount());
        if (cmd.getPassedCount() != null) entity.setPassedCount(cmd.getPassedCount());
        if (cmd.getBoxCount() != null) entity.setBoxCount(cmd.getBoxCount());
        if (cmd.getBoxLengthCm() != null) entity.setBoxLengthCm(cmd.getBoxLengthCm());
        if (cmd.getBoxWidthCm() != null) entity.setBoxWidthCm(cmd.getBoxWidthCm());
        if (cmd.getBoxHeightCm() != null) entity.setBoxHeightCm(cmd.getBoxHeightCm());
        if (cmd.getNetWeightPerUnit() != null) entity.setNetWeightPerUnit(cmd.getNetWeightPerUnit());
        if (cmd.getGrossWeight() != null) entity.setGrossWeight(cmd.getGrossWeight());
        if (cmd.getTaxInclusivePrice() != null) entity.setTaxInclusivePrice(cmd.getTaxInclusivePrice());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getTaxRefund() != null) entity.setTaxRefund(cmd.getTaxRefund());
        if (cmd.getQcStandard() != null) entity.setQcStandard(cmd.getQcStandard());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getImages() != null) entity.setImages(cmd.getImages());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
    }

    public void copyUpdate(QcRecordUpdateCmd cmd, QcRecord entity) {
        if (cmd.getSellerName() != null) entity.setSellerName(cmd.getSellerName());
        if (cmd.getQcUserId() != null) entity.setQcUserId(cmd.getQcUserId());
        if (cmd.getQcType() != null) entity.setQcType(cmd.getQcType());
        if (cmd.getQcDate() != null) entity.setQcDate(cmd.getQcDate());
        if (cmd.getResult() != null) entity.setResult(cmd.getResult());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
        if (cmd.getInspectionCount() != null) entity.setInspectionCount(cmd.getInspectionCount());
        if (cmd.getPassedCount() != null) entity.setPassedCount(cmd.getPassedCount());
        if (cmd.getBoxCount() != null) entity.setBoxCount(cmd.getBoxCount());
        if (cmd.getBoxLengthCm() != null) entity.setBoxLengthCm(cmd.getBoxLengthCm());
        if (cmd.getBoxWidthCm() != null) entity.setBoxWidthCm(cmd.getBoxWidthCm());
        if (cmd.getBoxHeightCm() != null) entity.setBoxHeightCm(cmd.getBoxHeightCm());
        if (cmd.getNetWeightPerUnit() != null) entity.setNetWeightPerUnit(cmd.getNetWeightPerUnit());
        if (cmd.getGrossWeight() != null) entity.setGrossWeight(cmd.getGrossWeight());
        if (cmd.getTaxInclusivePrice() != null) entity.setTaxInclusivePrice(cmd.getTaxInclusivePrice());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getTaxRefund() != null) entity.setTaxRefund(cmd.getTaxRefund());
        if (cmd.getQcStandard() != null) entity.setQcStandard(cmd.getQcStandard());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getImages() != null) entity.setImages(cmd.getImages());
        if (cmd.getInspectionCount() != null || cmd.getPassedCount() != null) {
            entity.calculateDefectiveCount();
        }
    }
}
