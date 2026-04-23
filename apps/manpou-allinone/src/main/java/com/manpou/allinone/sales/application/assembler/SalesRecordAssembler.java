package com.manpou.allinone.sales.application.assembler;

import com.manpou.allinone.sales.application.dto.*;
import com.manpou.allinone.sales.domain.model.SalesRecord;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class SalesRecordAssembler {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SalesRecord toEntity(SalesRecordCreateCmd cmd) {
        SalesRecord entity = new SalesRecord();
        entity.generateRecordCode();
        copyCreate(cmd, entity);
        entity.setCurrentStock(cmd.getInitialStock() != null ? cmd.getInitialStock() : 0);
        entity.setSalesQuantity(0);
        entity.setReturnedQuantity(0);
        entity.setReturnRate(java.math.BigDecimal.ZERO);
        return entity;
    }

    public void copyCreate(SalesRecordCreateCmd cmd, SalesRecord entity) {
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getProductCode() != null) entity.setProductCode(cmd.getProductCode());
        if (cmd.getSubProductCode() != null) entity.setSubProductCode(cmd.getSubProductCode());
        if (cmd.getSalesChannel() != null) entity.setSalesChannel(cmd.getSalesChannel());
        if (cmd.getListingDate() != null) entity.setListingDate(cmd.getListingDate());
        if (cmd.getInitialStock() != null) entity.setInitialStock(cmd.getInitialStock());
        if (cmd.getSafetyStock() != null) entity.setSafetyStock(cmd.getSafetyStock());
        if (cmd.getSellingPriceJpy() != null) entity.setSellingPriceJpy(cmd.getSellingPriceJpy());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyUpdate(SalesRecordUpdateCmd cmd, SalesRecord entity) {
        if (cmd.getListingDate() != null) entity.setListingDate(cmd.getListingDate());
        if (cmd.getSafetyStock() != null) entity.setSafetyStock(cmd.getSafetyStock());
        if (cmd.getSellingPriceJpy() != null) entity.setSellingPriceJpy(cmd.getSellingPriceJpy());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public SalesRecordPageQuery toDto(SalesRecord entity) {
        return SalesRecordPageQuery.builder()
                .id(entity.getId())
                .recordCode(entity.getRecordCode())
                .procurementId(entity.getProcurementId())
                .productCode(entity.getProductCode())
                .subProductCode(entity.getSubProductCode())
                .salesChannel(entity.getSalesChannel())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .listingDate(entity.getListingDate())
                .initialStock(entity.getInitialStock())
                .currentStock(entity.getCurrentStock())
                .safetyStock(entity.getSafetyStock())
                .salesQuantity(entity.getSalesQuantity())
                .returnedQuantity(entity.getReturnedQuantity())
                .returnRate(entity.getReturnRate())
                .sellingPriceJpy(entity.getSellingPriceJpy())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime() != null ? entity.getCreateTime().format(DT_FMT) : null)
                .updateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(DT_FMT) : null)
                .build();
    }
}
