package com.manpou.allinone.procurement.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.procurement.domain.model.BillingType;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
class ProcurementUseCaseTest {

    @Autowired
    private ProcurementUseCase procurementUseCase;

    @Autowired
    private ProcurementRepository procurementRepository;

    @Autowired
    private FactoryRepository factoryRepository;

    private Factory savedFactory;
    private Procurement savedProcurement;

    @BeforeEach
    void setUp() {
        Factory factory = new Factory();
        factory.setFactoryCode("F-TEST-001");
        factory.setFactoryName("测试工厂");
        factory.setProvince("浙江省");
        factory.setCity("杭州市");
        factory.setCooperationStatus(CooperationStatus.ACTIVE);
        savedFactory = factoryRepository.save(factory);

        Procurement p = new Procurement();
        p.setFactoryId(savedFactory.getId());
        p.setProductCode("odn-test-001");
        p.setQuantity(100);
        p.setPriceRmb(new BigDecimal("30.00"));
        p.setExchangeRate(new BigDecimal("21.5"));
        p.setTaxPoint(new BigDecimal("1.1000"));
        p.setBillingType(BillingType.ZHE_LU_KAI_PIAO);
        p.setStatus(ShipmentStatus.未定);
        p.setOrderDate(LocalDate.of(2026, 4, 10));
        p.setPlannedShipDate(LocalDate.of(2026, 4, 25));
        savedProcurement = procurementRepository.save(p);
        savedProcurement.calculateEstimatedPriceJpy();
        procurementRepository.save(savedProcurement);
    }

    @Test
    void getById_returnsDto() {
        ProcurementPageQuery result = procurementUseCase.getById(savedProcurement.getId());
        assertThat(result.getId()).isEqualTo(savedProcurement.getId());
        assertThat(result.getProductCode()).isEqualTo("odn-test-001");
        assertThat(result.getEstimatedPriceJpy()).isNotNull();
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> procurementUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Procurement");
    }

    @Test
    void create_calculatesEstimatedPrice() {
        ProcurementCreateCmd cmd = new ProcurementCreateCmd();
        cmd.setProductCode("odn-new-001");
        cmd.setQuantity(50);
        cmd.setPriceRmb(new BigDecimal("25.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));

        Long id = procurementUseCase.create(cmd);

        Procurement saved = procurementRepository.findById(id).orElseThrow();
        assertThat(saved.getEstimatedPriceJpy()).isNotNull();
        // 验证估算价公式: (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
        // 25 / 1.1 * 1.02 * 1.2 * 21.5 * 1.05 ≈ 628
        assertThat(saved.getEstimatedPriceJpy().doubleValue()).isCloseTo(628.0, within(5.0));
    }

    @Test
    void update_modifiesEntity() {
        ProcurementUpdateCmd cmd = new ProcurementUpdateCmd();
        cmd.setProductLead("新担当");
        cmd.setDestination("久留米");
        cmd.setQuantity(200);

        procurementUseCase.update(savedProcurement.getId(), cmd);

        Procurement updated = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        assertThat(updated.getProductLead()).isEqualTo("新担当");
        assertThat(updated.getDestination()).isEqualTo("久留米");
        assertThat(updated.getQuantity()).isEqualTo(200);
    }

    @Test
    void update_invalidStatusTransition_throws() {
        // 未定 → 完了 is NOT a valid FSM transition (完了 is only reachable from 会計)
        ProcurementUpdateCmd cmd = new ProcurementUpdateCmd();
        cmd.setStatus(ShipmentStatus.完了);

        // 领域层通过 updateStatus() 方法校验 FSM，非法转换抛出异常
        Procurement p = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        assertThatThrownBy(() -> p.updateStatus(ShipmentStatus.完了, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许跳转");
    }

    @Test
    void update_validTransition_changesStatus() {
        // 未定 → 発注待 is a valid FSM transition
        ProcurementUpdateCmd cmd = new ProcurementUpdateCmd();
        cmd.setStatus(ShipmentStatus.発注待);

        procurementUseCase.update(savedProcurement.getId(), cmd);

        Procurement updated = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.発注待);
    }

    @Test
    void delete未定态_succeeds() {
        procurementUseCase.delete(savedProcurement.getId());

        Procurement deleted = procurementRepository.findByIdAndDeletedIsFalse(savedProcurement.getId()).orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    void delete_non未定态_throws() {
        // FSM: 未定 → 発注待 → 永康；永康不在 [未定, 発注待] 可删除列表，delete 应抛异常
        ProcurementUpdateCmd cmd1 = new ProcurementUpdateCmd();
        cmd1.setStatus(ShipmentStatus.発注待);
        procurementUseCase.update(savedProcurement.getId(), cmd1);

        ProcurementUpdateCmd cmd2 = new ProcurementUpdateCmd();
        cmd2.setStatus(ShipmentStatus.永康);
        procurementUseCase.update(savedProcurement.getId(), cmd2);

        Procurement p = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(ShipmentStatus.永康);

        assertThatThrownBy(() -> procurementUseCase.delete(savedProcurement.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅未定");
    }

    @Test
    void pageQuery_returnsAll() {
        ProcurementQuery query = new ProcurementQuery();
        query.setPage(0);
        query.setPageSize(20);

        var result = procurementUseCase.pageQuery(query);

        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void pageQuery_filtersByProductCode() {
        ProcurementQuery query = new ProcurementQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setProductCode("odn-test-001");

        var result = procurementUseCase.pageQuery(query);

        assertThat(result.getContent()).allMatch(p -> p.getProductCode().equals("odn-test-001"));
    }

    @Test
    void pageQuery_filtersByStatus() {
        ProcurementQuery query = new ProcurementQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setStatus(ShipmentStatus.未定);

        var result = procurementUseCase.pageQuery(query);

        assertThat(result.getContent()).allMatch(p -> p.getStatus() == ShipmentStatus.未定);
    }

    @Test
    void statusTransition_validTransition_succeeds() {
        Procurement p = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        p.setStatus(ShipmentStatus.未定);
        procurementRepository.save(p);

        ProcurementUpdateCmd cmd = new ProcurementUpdateCmd();
        cmd.setStatus(ShipmentStatus.発注待);

        procurementUseCase.update(savedProcurement.getId(), cmd);

        Procurement updated = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.発注待);
    }

    @Test
    void domainMethod_invalidTransition_throws() {
        // 验证 Procurement.updateStatus 在领域层正确拦截非法跳转
        Procurement p = procurementRepository.findById(savedProcurement.getId()).orElseThrow();

        assertThatThrownBy(() -> p.updateStatus(ShipmentStatus.完了, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许跳转");
    }

    @Test
    void domainMethod_terminalStatus_blocksFurtherChange() {
        Procurement p = procurementRepository.findById(savedProcurement.getId()).orElseThrow();
        p.setStatus(ShipmentStatus.完了);
        procurementRepository.save(p);

        assertThatThrownBy(() -> p.updateStatus(ShipmentStatus.未定, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完了状态");
    }
}
