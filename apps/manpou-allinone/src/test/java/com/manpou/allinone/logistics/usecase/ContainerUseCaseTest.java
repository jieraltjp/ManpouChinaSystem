package com.manpou.allinone.logistics.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.ContainerUseCase;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * ContainerUseCase 集成测试（SPEC-B14 精简版）。
 * 移除了 addPlan/createFromPool/poolId 相关测试。
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

    private Long createContainer() {
        ContainerCreateCmd cmd = new ContainerCreateCmd();
        cmd.setContainerNo("TEST" + System.nanoTime());
        return containerUseCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        Long id = createContainer();
        assertThat(id).isNotNull();
        var dto = containerUseCase.getById(id);
        assertThat(dto.getContainerNo()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(ContainerStatus.CREATED);
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
        var dto = containerUseCase.getById(id);
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
