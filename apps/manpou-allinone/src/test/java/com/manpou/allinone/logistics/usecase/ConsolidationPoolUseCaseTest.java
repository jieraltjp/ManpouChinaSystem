package com.manpou.allinone.logistics.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolCreateCmd;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolPageQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolUpdateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.usecase.ConsolidationPoolUseCase;
import com.manpou.allinone.logistics.application.usecase.LogisticsPlanUseCase;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import com.manpou.allinone.logistics.domain.model.LogisticsPlan;
import com.manpou.allinone.logistics.domain.model.PlanType;
import com.manpou.allinone.logistics.domain.repository.ConsolidationPoolRepository;
import com.manpou.allinone.logistics.domain.repository.LogisticsPlanRepository;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
class ConsolidationPoolUseCaseTest {

    @Autowired
    private ConsolidationPoolUseCase poolUseCase;

    @Autowired
    private ConsolidationPoolRepository poolRepository;

    @Autowired
    private LogisticsPlanRepository logisticsPlanRepository;

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
        cmd.setProductCode("PT" + System.nanoTime());
        cmd.setQuantity(100);
        cmd.setPriceRmb(new BigDecimal("30.00"));
        cmd.setExchangeRate(new BigDecimal("21.5"));
        cmd.setTaxPoint(new BigDecimal("1.1000"));
        return procurementUseCase.create(cmd);
    }

    private Long createLogisticsPlan() {
        LogisticsPlanCreateCmd cmd = new LogisticsPlanCreateCmd();
        cmd.setProcurementId(createProcurement());
        cmd.setProductCode("odn-pool-test-001");
        cmd.setPlanType(PlanType.CONSOLIDATION);
        cmd.setCargoLengthCm(new BigDecimal("100"));
        cmd.setCargoWidthCm(new BigDecimal("100"));
        cmd.setCargoHeightCm(new BigDecimal("100"));
        cmd.setCargoWeightKg(new BigDecimal("50.0"));
        return logisticsPlanUseCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        ConsolidationPoolCreateCmd cmd = new ConsolidationPoolCreateCmd();
        cmd.setDestinationPort("久留米");

        Long id = poolUseCase.create(cmd);

        assertThat(id).isNotNull();
        ConsolidationPoolPageQuery dto = poolUseCase.getById(id);
        assertThat(dto.getPoolCode()).startsWith("CP-");
        assertThat(dto.getDestinationPort()).isEqualTo("久留米");
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.OPEN);
        assertThat(dto.getTotalCbm()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTotalWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPlanCount()).isEqualTo(0);
    }

    @Test
    void create_withThreshold_setsThreshold() {
        ConsolidationPoolCreateCmd cmd = new ConsolidationPoolCreateCmd();
        cmd.setDestinationPort("东京");
        cmd.setContainerThresholdCbm(new BigDecimal("50"));

        Long id = poolUseCase.create(cmd);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(id);
        assertThat(dto.getContainerThresholdCbm()).isEqualByComparingTo(new BigDecimal("50"));
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> poolUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("拼柜池不存在");
    }

    @Test
    void update_changesFields() {
        Long id = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        ConsolidationPoolUpdateCmd cmd = new ConsolidationPoolUpdateCmd();
        cmd.setContainerThresholdCbm(new BigDecimal("60"));

        poolUseCase.update(id, cmd);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(id);
        assertThat(dto.getContainerThresholdCbm()).isEqualByComparingTo(new BigDecimal("60"));
    }

    @Test
    void update_shippedStatus_throws() {
        Long id = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(id).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.SHIPPED);
            poolRepository.save(p);
        });

        assertThatThrownBy(() -> poolUseCase.update(id, new ConsolidationPoolUpdateCmd()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止修改");
    }

    @Test
    void delete_openStatus_succeeds() {
        Long id = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolUseCase.delete(id);

        assertThatThrownBy(() -> poolUseCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_nonOpenStatus_throws() {
        Long id = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(id).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.LOADED);
            poolRepository.save(p);
        });

        assertThatThrownBy(() -> poolUseCase.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅 OPEN 状态允许删除");
    }

    // ===== addPlan / removePlan =====

    @Test
    void addPlan_updatesVolumeAndWeight() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolUseCase.addPlan(poolId, planId);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        // 100×100×100 / 1,000,000 = 1.0 m³
        assertThat(dto.getTotalCbm()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(dto.getTotalWeightKg()).isEqualByComparingTo(new BigDecimal("50.0"));
        assertThat(dto.getPlanCount()).isEqualTo(1);
    }

    @Test
    void addPlan_bindsPlanToPool() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolUseCase.addPlan(poolId, planId);

        LogisticsPlanPageQuery planDto = logisticsPlanUseCase.getById(planId);
        assertThat(planDto.getPoolId()).isEqualTo(poolId);
    }

    @Test
    void addPlan_reachesThreshold_autoAdvancesToPending() {
        // 创建一个阈值为 1.0 的池，加入体积为 1.0 的计划，触发封池
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
            setContainerThresholdCbm(new BigDecimal("1.0"));
        }});

        poolUseCase.addPlan(poolId, planId);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.PENDING);
    }

    @Test
    void addPlan_belowThreshold_staysOpen() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
            setContainerThresholdCbm(new BigDecimal("10.0")); // 阈值 10，体积只有 1
        }});

        poolUseCase.addPlan(poolId, planId);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.OPEN);
    }

    @Test
    void addPlan_nonOpenPool_throws() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.SHIPPED);
            poolRepository.save(p);
        });

        assertThatThrownBy(() -> poolUseCase.addPlan(poolId, planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非 OPEN");
    }

    @Test
    void addPlan_planNotFound_throws() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        assertThatThrownBy(() -> poolUseCase.addPlan(poolId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调配计划不存在");
    }

    @Test
    void removePlan_reducesVolumeAndWeight() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolUseCase.addPlan(poolId, planId);

        poolUseCase.removePlan(poolId, planId);

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getTotalCbm()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTotalWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPlanCount()).isEqualTo(0);
    }

    @Test
    void removePlan_unbindsPlan() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolUseCase.addPlan(poolId, planId);

        poolUseCase.removePlan(poolId, planId);

        LogisticsPlanPageQuery planDto = logisticsPlanUseCase.getById(planId);
        assertThat(planDto.getPoolId()).isNull();
    }

    @Test
    void removePlan_loadedStatus_throws() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolUseCase.addPlan(poolId, planId);
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.LOADED);
            poolRepository.save(p);
        });

        assertThatThrownBy(() -> poolUseCase.removePlan(poolId, planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已装柜");
    }

    @Test
    void removePlan_shippedStatus_throws() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolUseCase.addPlan(poolId, planId);
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.SHIPPED);
            poolRepository.save(p);
        });

        assertThatThrownBy(() -> poolUseCase.removePlan(poolId, planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已出港");
    }

    // ===== 领域方法 =====

    @Test
    void isReadyToLoad_trueWhenAtOrAboveThreshold() {
        Long planId = createLogisticsPlan();
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
            setContainerThresholdCbm(new BigDecimal("1.0"));
        }});

        poolRepository.findById(poolId).ifPresent(p -> {
            p.addPlan(new BigDecimal("1.0"), new BigDecimal("50.0"));
            assertThat(p.isReadyToLoad()).isTrue();
            poolRepository.save(p);
        });
    }

    @Test
    void isReadyToLoad_falseWhenBelowThreshold() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
            setContainerThresholdCbm(new BigDecimal("10.0"));
        }});

        poolRepository.findById(poolId).ifPresent(p -> {
            p.addPlan(new BigDecimal("0.5"), new BigDecimal("25.0"));
            assertThat(p.isReadyToLoad()).isFalse();
            poolRepository.save(p);
        });
    }

    @Test
    void closeToPending_openToPending() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolRepository.findById(poolId).ifPresent(p -> {
            p.closeToPending();
            poolRepository.save(p);
        });

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.PENDING);
    }

    @Test
    void closeToPending_nonOpen_throws() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.PENDING);
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            assertThatThrownBy(p::closeToPending)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非 OPEN");
        });
    }

    @Test
    void markLoaded_pendingToLoaded() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.PENDING);
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            p.markLoaded();
            poolRepository.save(p);
        });

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.LOADED);
    }

    @Test
    void markShipped_loadedToShipped() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.LOADED);
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            p.markShipped();
            poolRepository.save(p);
        });

        ConsolidationPoolPageQuery dto = poolUseCase.getById(poolId);
        assertThat(dto.getStatus()).isEqualTo(ConsolidationPoolStatus.SHIPPED);
    }

    // ===== FSM =====

    @Test
    void fsm_open_to_pending() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolRepository.findById(poolId).ifPresent(p -> {
            p.advanceStatus(ConsolidationPoolStatus.PENDING);
            poolRepository.save(p);
        });

        assertThat(poolUseCase.getById(poolId).getStatus()).isEqualTo(ConsolidationPoolStatus.PENDING);
    }

    @Test
    void fsm_open_to_loaded() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});

        poolRepository.findById(poolId).ifPresent(p -> {
            p.advanceStatus(ConsolidationPoolStatus.LOADED);
            poolRepository.save(p);
        });

        assertThat(poolUseCase.getById(poolId).getStatus()).isEqualTo(ConsolidationPoolStatus.LOADED);
    }

    @Test
    void fsm_pending_cannotGoBackToOpen() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.PENDING);
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            assertThatThrownBy(() -> p.advanceStatus(ConsolidationPoolStatus.OPEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许跳转");
        });
    }

    @Test
    void fsm_shipped_isTerminal() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.LOADED);
            p.markShipped();
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            assertThat(p.getStatus().isTerminal()).isTrue();
        });
    }

    @Test
    void fsm_shipped_cannotTransition() {
        Long poolId = poolUseCase.create(new ConsolidationPoolCreateCmd() {{
            setDestinationPort("久留米");
        }});
        poolRepository.findById(poolId).ifPresent(p -> {
            p.setStatus(ConsolidationPoolStatus.SHIPPED);
            poolRepository.save(p);
        });

        poolRepository.findById(poolId).ifPresent(p -> {
            assertThatThrownBy(() -> p.advanceStatus(ConsolidationPoolStatus.OPEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许跳转");
        });
    }
}
