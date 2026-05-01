package com.manpou.allinone.customs.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.customs.application.dto.CustomsBatchCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.usecase.CustomsUseCase;
import com.manpou.allinone.customs.domain.model.DomesticCustomsStatus;
import com.manpou.allinone.customs.domain.model.DomesticCustomsRecord;
import com.manpou.allinone.customs.domain.repository.DomesticCustomsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * CustomsUseCase（国内报关）集成测试。
 *
 * FSM 状态机：
 *   PENDING → SUBMITTED → CLEARED（终态）
 *                    → REJECTED（可修正后重新提交）
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
class CustomsUseCaseTest {

    @Autowired
    private CustomsUseCase useCase;

    @Autowired
    private DomesticCustomsRepository repository;

    private Long createDomesticCustoms() {
        CustomsCreateCmd cmd = new CustomsCreateCmd();
        cmd.setContainerNo("DC" + System.nanoTime());
        cmd.setProductCode("DC-TEST");
        cmd.setQuantity(100);
        return useCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        Long id = createDomesticCustoms();

        assertThat(id).isNotNull();
        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getCustomsCode()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(DomesticCustomsStatus.PENDING);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> useCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void update_changesFields() {
        Long id = createDomesticCustoms();

        CustomsUpdateCmd cmd = new CustomsUpdateCmd();
        cmd.setEstimatedValueCny(new BigDecimal("50000"));

        useCase.update(id, cmd);

        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getEstimatedValueCny()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void delete_succeeds() {
        Long id = createDomesticCustoms();
        useCase.delete(id);

        assertThatThrownBy(() -> useCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    // ===== submit / clear / reject =====

    @Test
    void submit_pending_to_submitted() {
        Long id = createDomesticCustoms();

        useCase.submit(id);

        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(DomesticCustomsStatus.SUBMITTED);
    }

    @Test
    void submit_cleared_throws() {
        Long id = createDomesticCustoms();
        useCase.submit(id);
        useCase.clear(id);

        assertThatThrownBy(() -> useCase.submit(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已通关");
    }

    @Test
    void clear_submitted_to_cleared() {
        Long id = createDomesticCustoms();
        useCase.submit(id);

        useCase.clear(id);

        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(DomesticCustomsStatus.CLEARED);
    }

    @Test
    void clear_pending_throws() {
        Long id = createDomesticCustoms();

        assertThatThrownBy(() -> useCase.clear(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须先提交");
    }

    @Test
    void reject_submitted_to_rejected() {
        Long id = createDomesticCustoms();
        useCase.submit(id);

        useCase.reject(id, "文件不清晰");

        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(DomesticCustomsStatus.REJECTED);
    }

    @Test
    void reject_cleared_throws() {
        Long id = createDomesticCustoms();
        useCase.submit(id);
        useCase.clear(id);

        assertThatThrownBy(() -> useCase.reject(id, "修正"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已通关");
    }

    @Test
    void submit_rejected_canResubmit() {
        Long id = createDomesticCustoms();
        useCase.submit(id);
        useCase.reject(id, "初次驳回");
        useCase.submit(id);

        CustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(DomesticCustomsStatus.SUBMITTED);
    }

    @Test
    void clear_cleared_isTerminal() {
        Long id = createDomesticCustoms();
        useCase.submit(id);
        useCase.clear(id);

        repository.findByIdAndDeletedIsFalse(id).ifPresent(entity -> {
            assertThat(entity.isTerminal()).isTrue();
        });
    }

    // ===== pageQuery =====

    @Test
    void pageQuery_byContainerNo_containsResult() {
        String unique = "DCF" + System.nanoTime();
        CustomsCreateCmd cmd = new CustomsCreateCmd();
        cmd.setContainerNo(unique);
        cmd.setProductCode("DC-TEST");
        cmd.setQuantity(50);
        useCase.create(cmd);

        CustomsQuery query = new CustomsQuery();
        query.setContainerNo(unique);
        useCase.pageQuery(query);

        assertThat(useCase.pageQuery(query).getContent()).isNotEmpty();
    }

    @Test
    void pageQuery_pagination() {
        for (int i = 0; i < 5; i++) {
            createDomesticCustoms();
        }

        CustomsQuery query = new CustomsQuery();
        query.setPage(1);
        query.setPageSize(2);

        assertThat(useCase.pageQuery(query).getContent()).isNotEmpty();
    }

    // ===== batchCreate =====

    @Test
    void batchCreate_emptyList_returnsEmpty() {
        CustomsBatchCreateCmd cmd = new CustomsBatchCreateCmd();
        cmd.setLogisticsPlanIds(List.of());
        cmd.setContainerNo("BATCH-TEST");

        List<Long> ids = useCase.batchCreate(cmd);
        assertThat(ids).isEmpty();
    }
}
