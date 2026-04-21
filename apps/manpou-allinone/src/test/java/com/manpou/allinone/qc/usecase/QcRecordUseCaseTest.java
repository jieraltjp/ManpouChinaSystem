package com.manpou.allinone.qc.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.qc.application.dto.QcRecordCreateCmd;
import com.manpou.allinone.qc.application.dto.QcRecordPageQuery;
import com.manpou.allinone.qc.application.dto.QcRecordQuery;
import com.manpou.allinone.qc.application.dto.QcRecordUpdateCmd;
import com.manpou.allinone.qc.application.usecase.QcRecordUseCase;
import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.model.QcType;
import com.manpou.allinone.qc.domain.repository.QcRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.config.import=optional:nacos:"
})
@Transactional
class QcRecordUseCaseTest {

    @Autowired
    private QcRecordUseCase qcRecordUseCase;

    @Autowired
    private QcRecordRepository qcRecordRepository;

    @Autowired
    private ProcurementUseCase procurementUseCase;

    private Long createProcurement() {
        ProcurementCreateCmd cmd = new ProcurementCreateCmd();
        cmd.setProductCode("odn-qc-test-" + System.nanoTime());
        cmd.setQuantity(100);
        cmd.setPriceRmb(new BigDecimal("30.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));
        return procurementUseCase.create(cmd);
    }

    @Test
    void create_generatesQcCode() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setQcType(QcType.ONSITE);
        cmd.setQcDate(LocalDate.of(2026, 4, 15));
        cmd.setResult(QcResult.PASS);
        cmd.setInspectionCount(100);
        cmd.setPassedCount(97);

        Long id = qcRecordUseCase.create(cmd);

        QcRecordPageQuery dto = qcRecordUseCase.getById(id);
        assertThat(dto.getQcCode()).startsWith("Q-");
        assertThat(dto.getProductCode()).isEqualTo("odn-qc-test-001");
        assertThat(dto.getResult()).isEqualTo(QcResult.PASS);
    }

    @Test
    void create_calculatesDefectiveCount() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setInspectionCount(100);
        cmd.setPassedCount(95);

        Long id = qcRecordUseCase.create(cmd);

        QcRecordPageQuery dto = qcRecordUseCase.getById(id);
        assertThat(dto.getDefectiveCount()).isEqualTo(5);
    }

    @Test
    void create_passedCountExceedsInspectionCount_throws() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setInspectionCount(100);
        cmd.setPassedCount(105);

        assertThatThrownBy(() -> qcRecordUseCase.create(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("合格数量不能大于检品数");
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> qcRecordUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验货记录不存在");
    }

    @Test
    void pageQuery_filtersByResult() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setResult(QcResult.PASS);
        cmd.setInspectionCount(50);
        cmd.setPassedCount(48);
        qcRecordUseCase.create(cmd);

        QcRecordQuery query = new QcRecordQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setResult(QcResult.PASS);

        var result = qcRecordUseCase.pageQuery(query);
        assertThat(result.getContent()).allMatch(r -> r.getResult() == QcResult.PASS);
    }

    @Test
    void pageQuery_filtersByProcurementId() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setResult(QcResult.FAIL);
        cmd.setInspectionCount(50);
        cmd.setPassedCount(30);
        qcRecordUseCase.create(cmd);

        QcRecordQuery query = new QcRecordQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setProcurementId(procurementId);

        var result = qcRecordUseCase.pageQuery(query);
        assertThat(result.getContent()).allMatch(r -> r.getProcurementId().equals(procurementId));
    }

    @Test
    void updateStatus_toCompleted_succeeds() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd createCmd = new QcRecordCreateCmd();
        createCmd.setProcurementId(procurementId);
        createCmd.setProductCode("odn-qc-test-001");
        createCmd.setResult(QcResult.PASS);
        createCmd.setInspectionCount(50);
        createCmd.setPassedCount(48);
        Long id = qcRecordUseCase.create(createCmd);

        QcRecordUpdateCmd updateCmd = new QcRecordUpdateCmd();
        updateCmd.setStatus(QcStatus.COMPLETED);
        qcRecordUseCase.update(id, updateCmd);

        QcRecordPageQuery dto = qcRecordUseCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(QcStatus.COMPLETED);
    }

    @Test
    void delete_pendingRecord_succeeds() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setInspectionCount(10);
        Long id = qcRecordUseCase.create(cmd);

        qcRecordUseCase.delete(id);

        assertThatThrownBy(() -> qcRecordUseCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_nonPendingRecord_throws() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd createCmd = new QcRecordCreateCmd();
        createCmd.setProcurementId(procurementId);
        createCmd.setProductCode("odn-qc-test-001");
        createCmd.setResult(QcResult.PASS);
        createCmd.setInspectionCount(50);
        createCmd.setPassedCount(48);
        Long id = qcRecordUseCase.create(createCmd);

        QcRecordUpdateCmd updateCmd = new QcRecordUpdateCmd();
        updateCmd.setStatus(QcStatus.COMPLETED);
        qcRecordUseCase.update(id, updateCmd);

        assertThatThrownBy(() -> qcRecordUseCase.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待验货状态可删除");
    }

    @Test
    void updateResult_reevaluatesDefectiveCount() {
        Long procurementId = createProcurement();
        QcRecordCreateCmd cmd = new QcRecordCreateCmd();
        cmd.setProcurementId(procurementId);
        cmd.setProductCode("odn-qc-test-001");
        cmd.setInspectionCount(100);
        cmd.setPassedCount(95);
        cmd.setResult(QcResult.PASS);
        Long id = qcRecordUseCase.create(cmd);

        QcRecordUpdateCmd updateCmd = new QcRecordUpdateCmd();
        updateCmd.setPassedCount(90);
        qcRecordUseCase.update(id, updateCmd);

        QcRecordPageQuery dto = qcRecordUseCase.getById(id);
        assertThat(dto.getDefectiveCount()).isEqualTo(10);
    }
}
