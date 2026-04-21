package com.manpou.allinone.logistics.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.LogisticsPlanUseCase;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
class LogisticsPlanUseCaseTest {

    @Autowired
    private LogisticsPlanUseCase logisticsPlanUseCase;

    @Autowired
    private LogisticsPlanRepository logisticsPlanRepository;

    @Autowired
    private ProcurementUseCase procurementUseCase;

    private Long createProcurement() {
        ProcurementCreateCmd cmd = new ProcurementCreateCmd();
        cmd.setProductCode("odn-log-test-" + System.nanoTime());
        cmd.setQuantity(100);
        cmd.setPriceRmb(new BigDecimal("30.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));
        return procurementUseCase.create(cmd);
    }

    private Long createLogisticsPlan() {
        LogisticsPlanCreateCmd cmd = new LogisticsPlanCreateCmd();
        cmd.setProcurementId(createProcurement());
        cmd.setProductCode("odn-log-test-001");
        cmd.setPlanType(PlanType.SEA);
        cmd.setCargoLengthCm(new BigDecimal("50"));
        cmd.setCargoWidthCm(new BigDecimal("40"));
        cmd.setCargoHeightCm(new BigDecimal("30"));
        cmd.setCargoWeightKg(new BigDecimal("25.5"));
        return logisticsPlanUseCase.create(cmd);
    }

    @Test
    void create_generatesPlanCode() {
        Long id = createLogisticsPlan();

        LogisticsPlanPageQuery dto = logisticsPlanUseCase.getById(id);
        assertThat(dto.getPlanCode()).startsWith("L-");
        assertThat(dto.getProductCode()).isEqualTo("odn-log-test-001");
        assertThat(dto.getPlanType()).isEqualTo(PlanType.SEA);
        assertThat(dto.getStatus()).isEqualTo(LogisticsStatus.PLANNED);
    }

    @Test
    void create_calculatesVolume() {
        LogisticsPlanCreateCmd cmd = new LogisticsPlanCreateCmd();
        cmd.setProcurementId(createProcurement());
        cmd.setProductCode("odn-log-test-001");
        cmd.setPlanType(PlanType.SEA);
        cmd.setCargoLengthCm(new BigDecimal("100"));
        cmd.setCargoWidthCm(new BigDecimal("100"));
        cmd.setCargoHeightCm(new BigDecimal("100"));
        // 100×100×100 / 1,000,000 = 1.0 m³

        Long id = logisticsPlanUseCase.create(cmd);

        LogisticsPlanPageQuery dto = logisticsPlanUseCase.getById(id);
        assertThat(dto.getCargoVolumeCbm().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> logisticsPlanUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调配计划不存在");
    }

    @Test
    void pageQuery_returnsAtLeastCreatedRecord() {
        Long id = createLogisticsPlan();

        LogisticsPlanQuery query = new LogisticsPlanQuery();
        query.setPage(0);
        query.setPageSize(20);

        var result = logisticsPlanUseCase.pageQuery(query);
        assertThat(result.getContent()).anyMatch(p -> p.getId().equals(id));
    }

    @Test
    void pageQuery_filtersByStatus() {
        createLogisticsPlan();

        LogisticsPlanQuery query = new LogisticsPlanQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setStatus(LogisticsStatus.PLANNED);

        var result = logisticsPlanUseCase.pageQuery(query);
        assertThat(result.getContent()).allMatch(p -> p.getStatus() == LogisticsStatus.PLANNED);
    }

    @Test
    void pageQuery_filtersByPlanType() {
        createLogisticsPlan();

        LogisticsPlanQuery query = new LogisticsPlanQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setPlanType(PlanType.SEA);

        var result = logisticsPlanUseCase.pageQuery(query);
        assertThat(result.getContent()).allMatch(p -> p.getPlanType() == PlanType.SEA);
    }

    @Test
    void updateStatus_toBooked_succeeds() {
        Long id = createLogisticsPlan();

        LogisticsPlanUpdateCmd updateCmd = new LogisticsPlanUpdateCmd();
        updateCmd.setStatus(LogisticsStatus.BOOKED);
        logisticsPlanUseCase.update(id, updateCmd);

        LogisticsPlanPageQuery dto = logisticsPlanUseCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(LogisticsStatus.BOOKED);
    }

    @Test
    void updateStatus_fullFsmTransition_succeeds() {
        Long id = createLogisticsPlan();

        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.BOOKED); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.IN_TRANSIT); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.DELIVERED); }});

        LogisticsPlanPageQuery dto = logisticsPlanUseCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(LogisticsStatus.DELIVERED);
    }

    @Test
    void updateStatus_afterDelivered_throws() {
        Long id = createLogisticsPlan();

        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.BOOKED); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.IN_TRANSIT); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.DELIVERED); }});

        LogisticsPlanUpdateCmd illegalUpdate = new LogisticsPlanUpdateCmd();
        illegalUpdate.setStatus(LogisticsStatus.BOOKED);

        assertThatThrownBy(() -> logisticsPlanUseCase.update(id, illegalUpdate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已完成");
    }

    @Test
    void delete_plannedRecord_succeeds() {
        Long id = createLogisticsPlan();

        logisticsPlanUseCase.delete(id);

        assertThatThrownBy(() -> logisticsPlanUseCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_bookedRecord_succeeds() {
        Long id = createLogisticsPlan();
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.BOOKED); }});

        logisticsPlanUseCase.delete(id);

        assertThatThrownBy(() -> logisticsPlanUseCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_deliveredRecord_throws() {
        Long id = createLogisticsPlan();
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.BOOKED); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.IN_TRANSIT); }});
        logisticsPlanUseCase.update(id, new LogisticsPlanUpdateCmd() {{ setStatus(LogisticsStatus.DELIVERED); }});

        assertThatThrownBy(() -> logisticsPlanUseCase.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已完成/运输中");
    }
}
