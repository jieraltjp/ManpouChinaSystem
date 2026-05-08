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
    void linkToProcurement_setsConfirmed() {
        demandUseCase.linkToProcurement(savedDemand.getId(), 123L);

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DemandStatus.CONFIRMED);
        assertThat(updated.getLinkedProcurementId()).isEqualTo(123L);
    }

    @Test
    void unlinkProcurement_restoresPending() {
        demandUseCase.linkToProcurement(savedDemand.getId(), 123L);
        demandUseCase.unlinkProcurement(savedDemand.getId());

        ReplenishmentDemand updated = demandRepository.findById(savedDemand.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DemandStatus.PENDING);
        assertThat(updated.getLinkedProcurementId()).isNull();
    }

    @Test
    void unlinkProcurement_notLinked_throws() {
        assertThatThrownBy(() -> demandUseCase.unlinkProcurement(savedDemand.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未关联");
    }

    @Test
    void delete_anyStatus_succeeds() {
        // v2.2.0: any status can be deleted
        demandUseCase.linkToProcurement(savedDemand.getId(), 999L);
        demandUseCase.delete(savedDemand.getId());

        ReplenishmentDemand deleted = demandRepository.findByIdAndDeletedIsFalse(savedDemand.getId()).orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    void pageQuery_filtersByProductCode() {
        ReplenishmentDemandQuery query = new ReplenishmentDemandQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setKeyword("odn999");

        var result = demandUseCase.pageQuery(query);

        assertThat(result.getContent())
                .allMatch(d -> d.getProductCode().contains("odn999"));
    }

    @Test
    void pageQuery_filtersByDemandType() {
        ReplenishmentDemandQuery query = new ReplenishmentDemandQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setDemandType(DemandType.REPLENISHMENT);

        var result = demandUseCase.pageQuery(query);

        assertThat(result.getContent())
                .allMatch(d -> d.getDemandType() == DemandType.REPLENISHMENT);
    }
}
