package com.manpou.allinone.customs.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.customs.application.dto.JapanCustomsCompleteCmd;
import com.manpou.allinone.customs.application.dto.JapanCustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.JapanCustomsFailCmd;
import com.manpou.allinone.customs.application.dto.JapanCustomsPageQuery;
import com.manpou.allinone.customs.application.dto.JapanCustomsQuery;
import com.manpou.allinone.customs.application.dto.JapanCustomsUpdateCmd;
import com.manpou.allinone.customs.application.usecase.JapanCustomsUseCase;
import com.manpou.allinone.customs.domain.model.JapanCustomsStatus;
import com.manpou.allinone.customs.domain.repository.JapanCustomsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * JapanCustomsUseCase 集成测试。
 *
 * FSM 状态机：
 *   PENDING → IN_PROGRESS → CLEARED（终态）
 *                       → FAILED（终态）
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
class JapanCustomsUseCaseTest {

    @Autowired
    private JapanCustomsUseCase useCase;

    @Autowired
    private JapanCustomsRepository repository;

    private Long createJapanCustoms() {
        JapanCustomsCreateCmd cmd = new JapanCustomsCreateCmd();
        cmd.setContainerNo("JP" + System.nanoTime());
        cmd.setProductCode("JP-TEST");
        return useCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        Long id = createJapanCustoms();

        assertThat(id).isNotNull();
        JapanCustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getCustomsEntryNo()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(JapanCustomsStatus.PENDING.name());
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> useCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void update_changesFields() {
        Long id = createJapanCustoms();

        JapanCustomsUpdateCmd cmd = new JapanCustomsUpdateCmd();
        cmd.setCustomsBroker("东京通关社");
        cmd.setArrivalPort("东京港");

        useCase.update(id, cmd);

        JapanCustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getCustomsBroker()).isEqualTo("东京通关社");
        assertThat(dto.getArrivalPort()).isEqualTo("东京港");
    }

    @Test
    void update_terminalStatus_throws() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);
        useCase.complete(id, new JapanCustomsCompleteCmd() {{
            setImportDutyPaid(new BigDecimal("5000"));
            setConsumptionTaxPaid(new BigDecimal("3000"));
            setClearanceDate(LocalDate.of(2026, 5, 1));
        }});

        assertThatThrownBy(() -> useCase.update(id, new JapanCustomsUpdateCmd()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止修改");
    }

    @Test
    void delete_succeeds() {
        Long id = createJapanCustoms();
        useCase.delete(id);

        assertThatThrownBy(() -> useCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_terminalStatus_throws() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);
        useCase.complete(id, new JapanCustomsCompleteCmd() {{
            setImportDutyPaid(new BigDecimal("5000"));
            setConsumptionTaxPaid(new BigDecimal("3000"));
            setClearanceDate(LocalDate.of(2026, 5, 1));
        }});

        assertThatThrownBy(() -> useCase.delete(id))
                .isInstanceOf(BusinessException.class);
    }

    // ===== FSM 状态流转 =====

    @Test
    void fsm_pending_to_inProgress() {
        Long id = createJapanCustoms();

        useCase.startClearance(id);

        JapanCustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(JapanCustomsStatus.IN_PROGRESS.name());
    }

    @Test
    void fsm_inProgress_to_cleared() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);

        JapanCustomsCompleteCmd cmd = new JapanCustomsCompleteCmd();
        cmd.setImportDutyPaid(new BigDecimal("8000"));
        cmd.setConsumptionTaxPaid(new BigDecimal("5000"));
        cmd.setClearanceDate(LocalDate.of(2026, 5, 1));
        useCase.complete(id, cmd);

        JapanCustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(JapanCustomsStatus.CLEARED.name());
        assertThat(dto.getImportDutyPaid()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(dto.getClearanceDate().toString()).isEqualTo("2026-05-01");
    }

    @Test
    void fsm_inProgress_to_failed() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);

        JapanCustomsFailCmd cmd = new JapanCustomsFailCmd();
        cmd.setReason("文件不完整");
        useCase.fail(id, cmd);

        JapanCustomsPageQuery dto = useCase.getById(id);
        assertThat(dto.getStatus()).isEqualTo(JapanCustomsStatus.FAILED.name());
    }

    @Test
    void fsm_pending_cannot_direct_to_cleared() {
        Long id = createJapanCustoms();

        JapanCustomsCompleteCmd cmd = new JapanCustomsCompleteCmd();
        cmd.setImportDutyPaid(new BigDecimal("8000"));
        cmd.setConsumptionTaxPaid(new BigDecimal("5000"));
        cmd.setClearanceDate(LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> useCase.complete(id, cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许完成");
    }

    @Test
    void fsm_pending_cannot_direct_to_failed() {
        Long id = createJapanCustoms();

        JapanCustomsFailCmd cmd = new JapanCustomsFailCmd();
        cmd.setReason("任意原因");

        assertThatThrownBy(() -> useCase.fail(id, cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许标记失败");
    }

    @Test
    void fsm_cleared_isTerminal() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);
        useCase.complete(id, new JapanCustomsCompleteCmd() {{
            setImportDutyPaid(new BigDecimal("8000"));
            setConsumptionTaxPaid(new BigDecimal("5000"));
            setClearanceDate(LocalDate.of(2026, 5, 1));
        }});

        repository.findByIdAndDeletedIsFalse(id).ifPresent(entity -> {
            assertThat(entity.getStatus().isTerminal()).isTrue();
        });
    }

    @Test
    void fsm_failed_isTerminal() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);
        useCase.fail(id, new JapanCustomsFailCmd() {{ setReason("文件缺失"); }});

        repository.findByIdAndDeletedIsFalse(id).ifPresent(entity -> {
            assertThat(entity.getStatus().isTerminal()).isTrue();
        });
    }

    @Test
    void fsm_cleared_cannot_transition() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);
        useCase.complete(id, new JapanCustomsCompleteCmd() {{
            setImportDutyPaid(new BigDecimal("8000"));
            setConsumptionTaxPaid(new BigDecimal("5000"));
            setClearanceDate(LocalDate.of(2026, 5, 1));
        }});

        repository.findByIdAndDeletedIsFalse(id).ifPresent(entity -> {
            assertThat(entity.getStatus().canTransitionTo(JapanCustomsStatus.IN_PROGRESS)).isFalse();
            assertThat(entity.getStatus().canTransitionTo(JapanCustomsStatus.FAILED)).isFalse();
        });
    }

    @Test
    void fsm_inProgress_cannot_goBackToPending() {
        Long id = createJapanCustoms();
        useCase.startClearance(id);

        repository.findByIdAndDeletedIsFalse(id).ifPresent(entity -> {
            assertThat(entity.getStatus().canTransitionTo(JapanCustomsStatus.PENDING)).isFalse();
        });
    }

    // ===== pageQuery =====

    @Test
    void pageQuery_byContainerNo() {
        Long id = createJapanCustoms();
        String containerNo = "JP" + System.nanoTime();

        // 创建另一个不同的货柜号
        JapanCustomsCreateCmd cmd2 = new JapanCustomsCreateCmd();
        cmd2.setContainerNo(containerNo);
        useCase.create(cmd2);

        JapanCustomsQuery query = new JapanCustomsQuery();
        query.setContainerNo(containerNo);
        query.setPage(0);
        query.setPageSize(10);

        JapanCustomsPageQuery dto = useCase.pageQuery(query).getContent().get(0);
        assertThat(dto.getContainerNo()).isEqualTo(containerNo);
    }

    @Test
    void pageQuery_notFound_returnsEmpty() {
        JapanCustomsQuery query = new JapanCustomsQuery();
        query.setContainerNo("NONEXISTENT-CN-99999");
        query.setPage(0);
        query.setPageSize(10);

        assertThat(useCase.pageQuery(query).getContent()).isEmpty();
    }
}
