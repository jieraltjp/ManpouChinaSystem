package com.manpou.allinone.replenishment.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandCmd;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandResponse;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.application.usecase.ReplenishmentDemandUseCase;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

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
class ReplenishmentDemandUseCaseTest {

    @Autowired
    private ReplenishmentDemandUseCase demandUseCase;

    @Autowired
    private ReplenishmentDemandRepository demandRepository;

    @Autowired
    private com.manpou.allinone.factory.domain.repository.FactoryRepository factoryRepository;

    private ReplenishmentDemand savedDemand;
    private Factory savedFactory;

    @BeforeEach
    void setUp() {
        Factory factory = new Factory();
        factory.setFactoryCode("FAC-TEST-001");
        factory.setFactoryName("测试工厂");
        factory.setCooperationStatus(CooperationStatus.ACTIVE);
        savedFactory = factoryRepository.save(factory);

        // v2.0.0: 一行 = 一个子货号
        ReplenishmentDemand d = new ReplenishmentDemand();
        d.setDemandCode("DM-20260424-T001");
        d.setDemandType(DemandType.REPLENISHMENT);
        d.setProductCode("odn999");
        d.setSubProductCode("odn999-be");
        d.setQuantity(50);
        d.setDestination("东京");
        d.setJapanLead("测试担当");
        d.setStatus(DemandStatus.PENDING);

        savedDemand = demandRepository.save(d);
    }

    @Test
    void getById_returnsDto() {
        ReplenishmentDemandPageQuery result = demandUseCase.getById(savedDemand.getId());
        assertThat(result.getId()).isEqualTo(savedDemand.getId());
        assertThat(result.getDemandCode()).isEqualTo("DM-20260424-T001");
        assertThat(result.getSubProductCode()).isEqualTo("odn999-be");
        assertThat(result.getQuantity()).isEqualTo(50);
        assertThat(result.getDestination()).isEqualTo("东京");
        assertThat(result.getStatus()).isEqualTo(DemandStatus.PENDING);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> demandUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ReplenishmentDemand");
    }

    @Test
    void create_savesEntity() {
        ReplenishmentDemandCreateCmd cmd = new ReplenishmentDemandCreateCmd();
        cmd.setDemandType(DemandType.NEW_PURCHASE);
        cmd.setProductCode("odn888");
        cmd.setSubProductCode("odn888-wh");
        cmd.setQuantity(20);
        cmd.setDestination("久留米");
        cmd.setJapanLead("新担当");

        Long id = demandUseCase.create(cmd);

        Optional<ReplenishmentDemand> saved = demandRepository.findById(id);
        assertThat(saved).isPresent();
        assertThat(saved.get().getProductCode()).isEqualTo("odn888");
        assertThat(saved.get().getSubProductCode()).isEqualTo("odn888-wh");
        assertThat(saved.get().getQuantity()).isEqualTo(20);
        assertThat(saved.get().getStatus()).isEqualTo(DemandStatus.PENDING);
    }

    @Test
    void update_modifiesEntity() {
        ReplenishmentDemandUpdateCmd cmd = new ReplenishmentDemandUpdateCmd();
        cmd.setJapanLead("修改后担当");
        cmd.setQuantity(100);

        demandUseCase.update(savedDemand.getId(), cmd);

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getJapanLead()).isEqualTo("修改后担当");
        assertThat(updated.getQuantity()).isEqualTo(100);
    }

    @Test
    void convertToProcurement_updatesStatus_1_to_1() {
        ConvertDemandCmd cmd = new ConvertDemandCmd();
        cmd.setFactoryId(savedFactory.getId());

        ConvertDemandResponse resp = demandUseCase.convertToProcurement(savedDemand.getId(), cmd);

        assertThat(resp.getDemandStatus()).isEqualTo(DemandStatus.CONVERTED);
        assertThat(resp.getLinkedProcurementId()).isNotNull();

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DemandStatus.CONVERTED);
        assertThat(updated.getLinkedProcurementId()).isEqualTo(resp.getLinkedProcurementId());
    }

    @Test
    void convertToProcurement_alreadyProcessed_throws() {
        ConvertDemandCmd cmd = new ConvertDemandCmd();
        cmd.setFactoryId(savedFactory.getId());

        demandUseCase.convertToProcurement(savedDemand.getId(), cmd);

        assertThatThrownBy(() -> demandUseCase.convertToProcurement(savedDemand.getId(), cmd))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_pendingDemand_succeeds() {
        demandUseCase.delete(savedDemand.getId());

        ReplenishmentDemand deleted = demandRepository.findByIdAndDeletedIsFalse(savedDemand.getId()).orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    void delete_convertedDemand_throws() {
        ConvertDemandCmd cmd = new ConvertDemandCmd();
        cmd.setFactoryId(savedFactory.getId());
        demandUseCase.convertToProcurement(savedDemand.getId(), cmd);

        assertThatThrownBy(() -> demandUseCase.delete(savedDemand.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pageQuery_filtersByStatus() {
        ReplenishmentDemandQuery query = new ReplenishmentDemandQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setStatus(DemandStatus.PENDING);

        var result = demandUseCase.pageQuery(query);

        assertThat(result.getContent()).allMatch(d -> d.getStatus() == DemandStatus.PENDING);
    }

    @Test
    void pageQuery_filtersByProductCode() {
        ReplenishmentDemandQuery query = new ReplenishmentDemandQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setProductCode("odn999");

        var result = demandUseCase.pageQuery(query);

        assertThat(result.getContent())
                .allMatch(d -> d.getProductCode().equals("odn999"));
    }

    @Test
    void revertConversion_restoresPendingStatus() {
        ConvertDemandCmd cmd = new ConvertDemandCmd();
        cmd.setFactoryId(savedFactory.getId());
        demandUseCase.convertToProcurement(savedDemand.getId(), cmd);

        demandUseCase.revertConversion(savedDemand.getId());

        ReplenishmentDemand reverted = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(reverted.getStatus()).isEqualTo(DemandStatus.PENDING);
        assertThat(reverted.getLinkedProcurementId()).isNull();
    }
}
