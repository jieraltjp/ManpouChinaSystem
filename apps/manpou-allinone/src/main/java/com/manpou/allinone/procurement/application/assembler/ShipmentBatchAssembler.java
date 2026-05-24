package com.manpou.allinone.procurement.application.assembler;

import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchCreateCmd;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchPageQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchUpdateCmd;
import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 出货批次 DTO ↔ Entity 转换器（SPEC-B11）。
 */
@Component
@RequiredArgsConstructor
public class ShipmentBatchAssembler {

    private final ProductRepository productRepository;
    private final ProcurementRepository procurementRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateBatchCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("SB-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public ShipmentBatchPageQuery toDto(ShipmentBatch entity) {
        // 通过 procurement.productCode = nameZh 查询商品，获取主货号/子货号/图片
        String productMasterCode = null;
        String productSubCode = null;
        String productImageUrl = null;
        if (entity.getProcurementId() != null) {
            var procOpt = procurementRepository.findByIdAndDeletedIsFalse(entity.getProcurementId());
            if (procOpt.isPresent()) {
                var proc = procOpt.get();
                Product product = productRepository.findByNameZhAndDeletedIsFalse(proc.getProductCode()).orElse(null);
                if (product == null && proc.getSubProductCode() != null && !proc.getSubProductCode().isBlank()) {
                    product = productRepository.findByMasterCodeAndSubCodeAndDeletedIsFalse(
                            proc.getProductCode(), proc.getSubProductCode()).orElse(null);
                }
                if (product != null) {
                    productMasterCode = product.getMasterCode();
                    productSubCode = product.getSubCode();
                    productImageUrl = product.getImageUrl();
                }
            }
        }
        return ShipmentBatchPageQuery.builder()
                .id(entity.getId())
                .procurementId(entity.getProcurementId())
                .batchCode(entity.getBatchCode())
                .shipmentQuantity(entity.getShipmentQuantity())
                .factoryShipDate(entity.getFactoryShipDate())
                .actualShipDate(entity.getActualShipDate())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateBy(entity.getUpdateBy())
                .updateTime(entity.getUpdateTime())
                .productMasterCode(productMasterCode)
                .productSubCode(productSubCode)
                .productImageUrl(productImageUrl)
                .build();
    }

    public ShipmentBatch toEntity(ShipmentBatchCreateCmd cmd) {
        ShipmentBatch entity = new ShipmentBatch();
        entity.setBatchCode(generateBatchCode());
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(ShipmentBatchCreateCmd cmd, ShipmentBatch entity) {
        if (cmd.getProcurementId() != null) entity.setProcurementId(cmd.getProcurementId());
        if (cmd.getBatchCode() != null) entity.setBatchCode(cmd.getBatchCode());
        if (cmd.getShipmentQuantity() != null) entity.setShipmentQuantity(cmd.getShipmentQuantity());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }

    public void copyToEntity(ShipmentBatchUpdateCmd cmd, ShipmentBatch entity) {
        if (cmd.getBatchCode() != null) entity.setBatchCode(cmd.getBatchCode());
        if (cmd.getShipmentQuantity() != null) entity.setShipmentQuantity(cmd.getShipmentQuantity());
        if (cmd.getFactoryShipDate() != null) entity.setFactoryShipDate(cmd.getFactoryShipDate());
        if (cmd.getActualShipDate() != null) entity.setActualShipDate(cmd.getActualShipDate());
        if (cmd.getStatus() != null && cmd.getStatus() != entity.getStatus()) entity.updateStatus(cmd.getStatus());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }
}
