package com.manpou.allinone.replenishment.usecase;

import com.manpou.allinone.common.exception.BusinessException;
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

    private ReplenishmentDemand savedDemand;

    @BeforeEach
    void setUp() {
        ReplenishmentDemand d = new ReplenishmentDemand();
        d.setDemandCode("DM-20260421-T001");
        d.setDemandType(DemandType.REPLENISHMENT);
        d.setProductCode("odn999");
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
        assertThat(result.getDemandCode()).isEqualTo("DM-20260421-T001");
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
        cmd.setQuantity(100);
        cmd.setDestination("大阪");
        cmd.setJapanLead("新担当");

        Long id = demandUseCase.create(cmd);

        Optional<ReplenishmentDemand> saved = demandRepository.findById(id);
        assertThat(saved).isPresent();
        assertThat(saved.get().getProductCode()).isEqualTo("odn888");
        assertThat(saved.get().getStatus()).isEqualTo(DemandStatus.PENDING);
    }

    @Test
    void update_modifiesEntity() {
        ReplenishmentDemandUpdateCmd cmd = new ReplenishmentDemandUpdateCmd();
        cmd.setQuantity(200);
        cmd.setDestination("福冈");

        demandUseCase.update(savedDemand.getId(), cmd);

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(200);
        assertThat(updated.getDestination()).isEqualTo("福冈");
    }

    @Test
    void convertToProcurement_updatesStatus() {
        demandUseCase.convertToProcurement(savedDemand.getId(), 1L);

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DemandStatus.CONVERTED);
        assertThat(updated.getLinkedProcurementId()).isEqualTo(1L);
    }

    @Test
    void convertToProcurement_alreadyProcessed_throws() {
        // 先转为采购
        demandUseCase.convertToProcurement(savedDemand.getId(), 1L);

        // 再次转换应抛出异常
        assertThatThrownBy(() -> demandUseCase.convertToProcurement(savedDemand.getId(), 2L))
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
        demandUseCase.convertToProcurement(savedDemand.getId(), 1L);

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

        assertThat(result.getContent()).allMatch(d -> d.getProductCode().equals("odn999"));
    }
}
