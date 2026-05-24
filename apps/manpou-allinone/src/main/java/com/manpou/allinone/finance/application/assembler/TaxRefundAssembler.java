package com.manpou.allinone.finance.application.assembler;

import com.manpou.allinone.finance.application.dto.*;
import com.manpou.allinone.finance.domain.model.TaxRefundRecord;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TaxRefundAssembler {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProcurementRepository procurementRepository;
    private final ProductRepository productRepository;

    public TaxRefundRecord toEntity(TaxRefundCreateCmd cmd) {
        TaxRefundRecord entity = new TaxRefundRecord();
        entity.generateRefundCode();
        copyCreate(cmd, entity);
        entity.calculateEstimatedRefund();
        return entity;
    }

    public void copyCreate(TaxRefundCreateCmd cmd, TaxRefundRecord entity) {
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getJapanCustomsId() != null) entity.setJapanCustomsId(cmd.getJapanCustomsId());
        if (cmd.getBillingType() != null) entity.setBillingType(cmd.getBillingType());
        if (cmd.getPriceRmb() != null) entity.setPriceRmb(cmd.getPriceRmb());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getTaxPoint() != null) entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getExchangeRate() != null) entity.setExchangeRate(cmd.getExchangeRate());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public TaxRefundPageQuery toDto(TaxRefundRecord entity) {
        TaxRefundPageQuery.TaxRefundPageQueryBuilder builder = TaxRefundPageQuery.builder()
                .id(entity.getId())
                .refundCode(entity.getRefundCode())
                .procurementId(entity.getProcurementId())
                .japanCustomsId(entity.getJapanCustomsId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .billingType(entity.getBillingType() != null ? entity.getBillingType().name() : null)
                .priceRmb(entity.getPriceRmb())
                .quantity(entity.getQuantity())
                .taxPoint(entity.getTaxPoint())
                .estimatedRefundRmb(entity.getEstimatedRefundRmb())
                .actualRefundRmb(entity.getActualRefundRmb())
                .exchangeRate(entity.getExchangeRate())
                .refundDate(entity.getRefundDate())
                .refundBank(entity.getRefundBank())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime() != null ? entity.getCreateTime().format(DT_FMT) : null)
                .updateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(DT_FMT) : null);

        // 通过 procurementId JOIN 获取产品信息
        if (entity.getProcurementId() != null) {
            procurementRepository.findByIdAndDeletedIsFalse(entity.getProcurementId())
                    .ifPresent(procurement -> {
                        builder.productCode(procurement.getProductCode());
                        builder.subProductCode(procurement.getSubProductCode());
                        // 通过 productCode 查图片
                        if (procurement.getProductCode() != null) {
                            productRepository.findByMasterCodeAndDeletedIsFalse(procurement.getProductCode())
                                    .ifPresent(product -> builder.productImageUrl(product.getImageUrl()));
                        }
                    });
        }

        return builder.build();
    }
}
