package com.manpou.allinone.factory.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.application.dto.FactoryQuery;
import com.manpou.allinone.factory.application.dto.FactoryUpdateCmd;
import com.manpou.allinone.factory.application.usecase.FactoryUseCase;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
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
class FactoryUseCaseTest {

    @Autowired
    private FactoryUseCase factoryUseCase;

    @Autowired
    private FactoryRepository factoryRepository;

    private Factory savedFactory;

    @BeforeEach
    void setUp() {
        Factory factory = new Factory();
        factory.setFactoryCode("F-20260401-TEST");
        factory.setFactoryName("测试箱包厂");
        factory.setLocation("浙江省杭州市");
        factory.setRoughLocation("萧山区工业园");
        factory.setContactName("测试联系人");
        factory.setContactPhone("13800000000");
        factory.setStatus(FactoryStatus.ACTIVE);
        savedFactory = factoryRepository.save(factory);
    }

    @Test
    void getById_returnsDto() {
        FactoryPageQuery result = factoryUseCase.getById(savedFactory.getId());
        assertThat(result.getId()).isEqualTo(savedFactory.getId());
        assertThat(result.getFactoryName()).isEqualTo("测试箱包厂");
        assertThat(result.getStatus()).isEqualTo(FactoryStatus.ACTIVE);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> factoryUseCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Factory");
    }

    @Test
    void create_savesEntity() {
        FactoryCreateCmd cmd = new FactoryCreateCmd();
        cmd.setFactoryName("新建工厂");
        cmd.setLocation("广东省深圳市");
        cmd.setRoughLocation("龙华区");
        cmd.setContactName("新建联系人");
        cmd.setContactPhone("13900000000");

        Long id = factoryUseCase.create(cmd);

        Optional<Factory> saved = factoryRepository.findById(id);
        assertThat(saved).isPresent();
        assertThat(saved.get().getFactoryName()).isEqualTo("新建工厂");
    }

    @Test
    void update_modifiesEntity() {
        FactoryUpdateCmd cmd = new FactoryUpdateCmd();
        cmd.setFactoryName("更新后的工厂名");
        cmd.setStatus(FactoryStatus.INACTIVE);

        factoryUseCase.update(savedFactory.getId(), cmd);

        Factory updated = factoryRepository.findById(savedFactory.getId()).orElseThrow();
        assertThat(updated.getFactoryName()).isEqualTo("更新后的工厂名");
        assertThat(updated.getStatus()).isEqualTo(FactoryStatus.INACTIVE);
    }

    @Test
    void update_notFound_throws() {
        FactoryUpdateCmd cmd = new FactoryUpdateCmd();
        cmd.setFactoryName("随便");

        assertThatThrownBy(() -> factoryUseCase.update(99999L, cmd))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_marksDeleted() {
        factoryUseCase.delete(savedFactory.getId());

        Factory deleted = factoryRepository.findByIdAndIsDeletedFalse(savedFactory.getId()).orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    void pageQuery_returnsAll() {
        FactoryQuery query = new FactoryQuery();
        query.setPage(0);
        query.setPageSize(20);

        var result = factoryUseCase.pageQuery(query);

        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result.getContent().get(0).getFactoryName()).isNotBlank();
    }

    @Test
    void pageQuery_filtersByStatus() {
        FactoryQuery query = new FactoryQuery();
        query.setPage(0);
        query.setPageSize(20);
        query.setStatus(FactoryStatus.ACTIVE);

        var result = factoryUseCase.pageQuery(query);

        assertThat(result.getContent()).allMatch(f -> f.getStatus() == FactoryStatus.ACTIVE);
    }
}
