package com.manpou.allinone.logistics.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.usecase.ContainerUseCase;
import com.manpou.allinone.logistics.application.usecase.LogisticsPlanUseCase;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.ContainerType;
import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import com.manpou.allinone.logistics.domain.repository.ConsolidationPoolRepository;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * ContainerUseCase 集成测试。
 * 注意：字段必填校验由 Controller 层 @Valid 处理，UseCase 层不重复校验。
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
class ContainerUseCaseTest {

    @Autowired
    private ContainerUseCase containerUseCase;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ConsolidationPoolRepository poolRepository;

    @Autowired
    private LogisticsPlanUseCase logisticsPlanUseCase;

    @Autowired
    private ProcurementUseCase procurementUseCase;

    @Autowired
    private FactoryRepository factoryRepository;

    private Factory createFactory() {
        Factory factory = new Factory();
        factory.setFactoryCode("F-TEST-" + System.nanoTime());
        factory.setFactoryName("测试工厂");
        factory.setProvince("浙江省");
        factory.setCity("杭州市");
        factory.setCooperationStatus(CooperationStatus.ACTIVE);
        return factoryRepository.save(factory);
    }

    private Long createProcurement() {
        Factory factory = createFactory();
        ProcurementCreateCmd cmd = new ProcurementCreateCmd();
        cmd.setFactoryId(factory.getId());
        cmd.setProductCode("CT" + System.nanoTime());
        cmd.setQuantity(100);
        cmd.setPriceRmb(new BigDecimal("30.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));
        return procurementUseCase.create(cmd);
    }

    private Long createLogisticsPlan() {
        LogisticsPlanCreateCmd cmd = new LogisticsPlanCreateCmd();
        cmd.setProcurementId(createProcurement());
        cmd.setProductCode("odn-container-test-001");
        cmd.setPlanType(PlanType.SEA);
        cmd.setCargoLengthCm(new BigDecimal("100"));
        cmd.setCargoWidthCm(new BigDecimal("100"));
        cmd.setCargoHeightCm(new BigDecimal("100"));
        cmd.setCargoWeightKg(new BigDecimal("25.5"));
        return logisticsPlanUseCase.create(cmd);
    }

    private Long createContainer() {
        ContainerCreateCmd cmd = new ContainerCreateCmd();
        cmd.setContainerNo("TEST" + System.nanoTime());
        cmd.setContainerType(ContainerType.GP20);
        return containerUseCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        Long id = createContainer();

        assertThat(id).isNotNull();
        ContainerPageQuery dto = containerUseCase.getById(id);
        assertThat(dto.getContainerNo()).isNotNull();
        assertThat(dto.getContainerType()).isEqualTo(ContainerType.GP20);
        assertThat(dto.getStatus()).isEqualTo(ContainerStatus.CREATED);
        assertThat(dto.getTotalCbm()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTotalWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPlanCount()).isEqualTo(0);
    }

    @Test
    void create_withPoolId_marksPoolAsLoaded() {
        var pool = new com.manpou.allinone.logistics.domain.model.ConsolidationPool();
        pool.setPoolCode("CPTEST" + System.nanoTime());
        pool.setDestinationPort("东京");
        pool = poolRepository.save(pool);
        Long poolId = pool.getId();

        ContainerCreateCmd cmd = new ContainerCreateCmd();
        cmd.setContainerNo("TP" + System.nanoTime());
        cmd.setContainerType(ContainerType.GP40);
        cmd.setPoolId(poolId);
        Long id = containerUseCase.create(cmd);

        var reloaded = poolRepository.findById(pool.getId()).orElseThrow();
        assertThat(reloaded.getStatus())
                .isEqualTo(com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus.LOADED);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> containerUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("货柜不存在");
    }

    @Test
    void update_changesFields() {
        Long id = createContainer();

        ContainerUpdateCmd cmd = new ContainerUpdateCmd();
        cmd.setLoadDate(java.time.LocalDate.of(2026, 5, 1));

        containerUseCase.update(id, cmd);

        ContainerPageQuery dto = containerUseCase.getById(id);
        assertThat(dto.getLoadDate().toString()).isEqualTo("2026-05-01");
    }

    @Test
    void update_terminalStatus_throws() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.setStatus(ContainerStatus.ARRIVED);
            containerRepository.save(c);
        });

        assertThatThrownBy(() -> containerUseCase.update(id, new ContainerUpdateCmd()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止修改");
    }

    @Test
    void delete_createdStatus_succeeds() {
        Long id = createContainer();
        containerUseCase.delete(id);

        assertThatThrownBy(() -> containerUseCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_nonCreatedStatus_throws() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.setStatus(ContainerStatus.LOADED);
            containerRepository.save(c);
        });

        assertThatThrownBy(() -> containerUseCase.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅 CREATED 状态允许删除");
    }

    // ===== addPlan =====

    @Test
    void addPlan_updatesVolumeAndWeight() {
        Long planId = createLogisticsPlan();
        Long containerId = createContainer();

        containerUseCase.addPlan(containerId, planId);

        ContainerPageQuery dto = containerUseCase.getById(containerId);
        assertThat(dto.getTotalCbm()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(dto.getTotalWeightKg()).isEqualByComparingTo(new BigDecimal("25.5"));
        assertThat(dto.getPlanCount()).isEqualTo(1);
    }

    @Test
    void addPlan_bindsPlanToContainer() {
        Long planId = createLogisticsPlan();
        Long containerId = createContainer();

        containerUseCase.addPlan(containerId, planId);

        LogisticsPlanPageQuery planDto = logisticsPlanUseCase.getById(planId);
        assertThat(planDto.getContainerId()).isEqualTo(containerId);
        assertThat(planDto.getContainerNo()).isNotNull();
    }

    @Test
    void addPlan_nonCreatedContainer_throws() {
        Long planId = createLogisticsPlan();
        Long containerId = createContainer();
        containerRepository.findById(containerId).ifPresent(c -> {
            c.setStatus(ContainerStatus.LOADED);
            containerRepository.save(c);
        });

        assertThatThrownBy(() -> containerUseCase.addPlan(containerId, planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非 CREATED");
    }

    @Test
    void addPlan_planNotFound_throws() {
        Long containerId = createContainer();

        assertThatThrownBy(() -> containerUseCase.addPlan(containerId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调配计划不存在");
    }

    @Test
    void addPlan_containerNotFound_throws() {
        Long planId = createLogisticsPlan();

        assertThatThrownBy(() -> containerUseCase.addPlan(99999L, planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("货柜不存在");
    }

    // ===== createFromPool =====

    @Test
    void createFromPool_createsContainerWithCorrectType() {
        var pool = new com.manpou.allinone.logistics.domain.model.ConsolidationPool();
        pool.setPoolCode("CPFROM" + System.nanoTime());
        pool.setDestinationPort("久留米");
        pool.setTotalCbm(new BigDecimal("80"));
        pool = poolRepository.save(pool);

        Long containerId = containerUseCase.createFromPool(pool.getId());

        ContainerPageQuery dto = containerUseCase.getById(containerId);
        assertThat(dto.getPoolId()).isEqualTo(pool.getId());
        assertThat(dto.getContainerType()).isEqualTo(ContainerType.GP40);
    }

    @Test
    void createFromPool_smallCbm_usesGP20() {
        var pool = new com.manpou.allinone.logistics.domain.model.ConsolidationPool();
        pool.setPoolCode("CPSMALL" + System.nanoTime());
        pool.setDestinationPort("大阪");
        pool.setTotalCbm(new BigDecimal("50"));
        pool = poolRepository.save(pool);

        Long containerId = containerUseCase.createFromPool(pool.getId());

        ContainerPageQuery dto = containerUseCase.getById(containerId);
        assertThat(dto.getContainerType()).isEqualTo(ContainerType.GP20);
    }

    @Test
    void createFromPool_poolNotFound_throws() {
        assertThatThrownBy(() -> containerUseCase.createFromPool(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("拼柜池不存在");
    }

    // ===== FSM 状态流转 =====

    @Test
    void fsm_created_to_loaded() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.advanceStatus(ContainerStatus.LOADED);
            containerRepository.save(c);
        });
        assertThat(containerUseCase.getById(id).getStatus()).isEqualTo(ContainerStatus.LOADED);
    }

    @Test
    void fsm_created_cannot_skip_to_departed() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            assertThatThrownBy(() -> c.advanceStatus(ContainerStatus.DEPARTED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许跳转");
        });
    }

    @Test
    void fsm_loaded_to_departed() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.setStatus(ContainerStatus.LOADED);
            containerRepository.save(c);
        });
        containerRepository.findById(id).ifPresent(c -> {
            c.advanceStatus(ContainerStatus.DEPARTED);
            containerRepository.save(c);
        });
        assertThat(containerUseCase.getById(id).getStatus()).isEqualTo(ContainerStatus.DEPARTED);
    }

    @Test
    void fsm_departed_to_arrived() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.setStatus(ContainerStatus.LOADED);
            c.advanceStatus(ContainerStatus.DEPARTED);
            containerRepository.save(c);
        });
        containerRepository.findById(id).ifPresent(c -> {
            c.advanceStatus(ContainerStatus.ARRIVED);
            containerRepository.save(c);
        });
        assertThat(containerUseCase.getById(id).getStatus()).isEqualTo(ContainerStatus.ARRIVED);
    }

    @Test
    void fsm_arrived_isTerminal() {
        Long id = createContainer();
        containerRepository.findById(id).ifPresent(c -> {
            c.setStatus(ContainerStatus.LOADED);
            c.advanceStatus(ContainerStatus.DEPARTED);
            c.advanceStatus(ContainerStatus.ARRIVED);
            containerRepository.save(c);
        });
        containerRepository.findById(id).ifPresent(c -> {
            assertThat(c.getStatus().isTerminal()).isTrue();
        });
    }
}
