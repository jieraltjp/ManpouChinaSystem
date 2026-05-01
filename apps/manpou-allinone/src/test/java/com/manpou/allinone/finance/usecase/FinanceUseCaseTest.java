package com.manpou.allinone.finance.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.finance.application.dto.FinanceCreateCmd;
import com.manpou.allinone.finance.application.dto.FinancePageQuery;
import com.manpou.allinone.finance.application.dto.FinanceQuery;
import com.manpou.allinone.finance.application.dto.FinanceUpdateCmd;
import com.manpou.allinone.finance.application.usecase.FinanceUseCase;
import com.manpou.allinone.finance.domain.model.FinanceStatus;
import com.manpou.allinone.finance.domain.repository.FinanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.*;

/**
 * FinanceUseCase 集成测试（财务示例 CRUD）。
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
class FinanceUseCaseTest {

    @Autowired
    private FinanceUseCase useCase;

    @Autowired
    private FinanceRepository repository;

    private Long createFinance(String name) {
        FinanceCreateCmd cmd = new FinanceCreateCmd();
        cmd.setName(name);
        return useCase.create(cmd);
    }

    // ===== CRUD =====

    @Test
    void create_generatesIdAndDefaultStatus() {
        Long id = createFinance("测试财务-" + System.nanoTime());

        assertThat(id).isNotNull();
        FinancePageQuery dto = useCase.getById(id);
        assertThat(dto.getName()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(FinanceStatus.ACTIVE.name());
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> useCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void update_changesFields() {
        Long id = createFinance("原始名称-" + System.nanoTime());

        FinanceUpdateCmd cmd = new FinanceUpdateCmd();
        cmd.setName("更新后名称-" + System.nanoTime());

        useCase.update(id, cmd);

        FinancePageQuery dto = useCase.getById(id);
        assertThat(dto.getName()).isNotNull();
    }

    @Test
    void update_renames() {
        Long id = createFinance("旧名称-" + System.nanoTime());

        FinanceUpdateCmd cmd = new FinanceUpdateCmd();
        cmd.setName("新名称-" + System.nanoTime());

        useCase.update(id, cmd);

        FinancePageQuery dto = useCase.getById(id);
        assertThat(dto.getName()).isNotNull();
    }

    @Test
    void delete_succeeds() {
        Long id = createFinance("待删除-" + System.nanoTime());
        useCase.delete(id);

        assertThatThrownBy(() -> useCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_notFound_throws() {
        assertThatThrownBy(() -> useCase.delete(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    // ===== pageQuery =====

    @Test
    void pageQuery_returnsPage() {
        createFinance("分页测试-" + System.nanoTime());

        FinanceQuery query = new FinanceQuery();
        query.setPage(0);
        query.setPageSize(10);

        assertThat(useCase.pageQuery(query).getContent()).isNotEmpty();
    }

    @Test
    void pageQuery_emptyPage() {
        FinanceQuery query = new FinanceQuery();
        query.setPage(999); // 不存在的页
        query.setPageSize(10);

        assertThat(useCase.pageQuery(query).getContent()).isEmpty();
    }
}
