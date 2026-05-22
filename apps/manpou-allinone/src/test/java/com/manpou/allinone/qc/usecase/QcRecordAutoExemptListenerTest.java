package com.manpou.allinone.qc.usecase;

import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.usecase.FactoryUseCase;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchCreateCmd;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.procurement.application.usecase.ShipmentBatchUseCase;
import com.manpou.allinone.procurement.domain.event.ShipmentBatchCreatedEvent;
import com.manpou.allinone.qc.application.listener.QcRecordAutoExemptListener;
import com.manpou.allinone.qc.application.usecase.QcRecordUseCase;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.model.QcType;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-B13 老厂家免验功能测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.config.import=optional:nacos:"
})
@Transactional
class QcRecordAutoExemptListenerTest {

    @Autowired
    private FactoryUseCase factoryUseCase;

    @Autowired
    private ProcurementUseCase procurementUseCase;

    @Autowired
    private ShipmentBatchUseCase shipmentBatchUseCase;

    @Autowired
    private QcRecordUseCase qcRecordUseCase;

    @Autowired
    private QcRecordRepository qcRecordRepository;

    @Autowired
    private QcRecordAutoExemptListener listener;

    private Long createFactory(boolean needsQc) {
        FactoryCreateCmd cmd = new FactoryCreateCmd();
        cmd.setFactoryName("免验测试工厂-" + UUID.randomUUID());
        cmd.setNeedsQc(needsQc);
        return factoryUseCase.create(cmd);
    }

    private Long createProcurement(Long factoryId) {
        ProcurementCreateCmd cmd = new ProcurementCreateCmd();
        cmd.setFactoryId(factoryId);
        cmd.setProductCode("TEST-" + UUID.randomUUID());
        cmd.setQuantity(100);
        cmd.setPriceRmb(new BigDecimal("30.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));
        return procurementUseCase.create(cmd);
    }

    @Test
    void createExemptQcRecord_needsQcFalse_createsExemptRecord() {
        // 准备：老厂家 + 采购单 + 出货批次
        Long factoryId = createFactory(false); // needsQc = false
        Long procurementId = createProcurement(factoryId);
        Long batchId = createShipmentBatch(procurementId);

        // 执行：直接调用 listener
        listener.createExemptQcRecord(new ShipmentBatchCreatedEvent(batchId, procurementId, factoryId));

        // 验证：验货记录已创建
        var qcRecords = qcRecordRepository.findByShipmentBatchIdAndDeletedIsFalse(batchId, PageRequest.of(0, 1));
        assertThat(qcRecords.getContent()).hasSize(1);
        var qcRecord = qcRecords.getContent().get(0);
        assertThat(qcRecord.getQcType()).isEqualTo(QcType.EXEMPT);
        assertThat(qcRecord.getResult()).isEqualTo(QcResult.PASS);
        assertThat(qcRecord.getStatus()).isEqualTo(QcStatus.COMPLETED);
        assertThat(qcRecord.getRemarks()).isEqualTo("老厂家免验");
        assertThat(qcRecord.getSellerName()).isNotNull();
    }

    @Test
    void createExemptQcRecord_needsQcTrue_noRecordCreated() {
        Long factoryId = createFactory(true); // needsQc = true（需要验货）
        Long procurementId = createProcurement(factoryId);
        Long batchId = createShipmentBatch(procurementId);

        listener.createExemptQcRecord(new ShipmentBatchCreatedEvent(batchId, procurementId, factoryId));

        var qcRecords = qcRecordRepository.findByShipmentBatchIdAndDeletedIsFalse(batchId, PageRequest.of(0, 1));
        assertThat(qcRecords.getContent()).isEmpty();
    }

    @Test
    void createExemptQcRecord_idempotent_calledTwice_createsOnlyOne() {
        Long factoryId = createFactory(false);
        Long procurementId = createProcurement(factoryId);
        Long batchId = createShipmentBatch(procurementId);

        listener.createExemptQcRecord(new ShipmentBatchCreatedEvent(batchId, procurementId, factoryId));
        listener.createExemptQcRecord(new ShipmentBatchCreatedEvent(batchId, procurementId, factoryId));

        var qcRecords = qcRecordRepository.findByShipmentBatchIdAndDeletedIsFalse(batchId, PageRequest.of(0, 10));
        assertThat(qcRecords.getContent()).hasSize(1); // 幂等：只创建一条
    }

    @Test
    void createExemptQcRecord_nullFactoryId_skips() {
        listener.createExemptQcRecord(new ShipmentBatchCreatedEvent(999L, null, null));

        // 无异常，不创建任何记录（因 factoryId 为 null）
        // （batchId 999 不存在，所以不会查到任何记录）
    }

    private Long createShipmentBatch(Long procurementId) {
        ShipmentBatchCreateCmd cmd = new ShipmentBatchCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setShipmentQuantity(100);
        return shipmentBatchUseCase.create(cmd);
    }
}
