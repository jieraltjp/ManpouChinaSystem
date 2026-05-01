package com.manpou.allinone.product.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.usecase.ProductUseCase;
import com.manpou.allinone.product.domain.model.ProductCategory;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductFactoryJpaRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductJpaRepository;
import com.manpou.allinone.product.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * ProductUseCase 集成测试。
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
class ProductUseCaseTest {

    @Autowired
    private ProductUseCase useCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductFactoryJpaRepository productFactoryJpaRepository;

    @Autowired
    private FactoryRepository factoryRepository;

    private Long createProduct() {
        ProductCreateCmd cmd = new ProductCreateCmd();
        cmd.setMasterCode("M" + System.nanoTime());
        cmd.setNameZh("测试商品");
        cmd.setCategory(ProductCategory.ORDINARY);
        return useCase.create(cmd);
    }

    private Long createSubProduct(String masterCode) {
        ProductCreateCmd cmd = new ProductCreateCmd();
        cmd.setMasterCode(masterCode);
        cmd.setSubCode("C-RED-" + System.nanoTime());
        cmd.setNameZh("子商品");
        cmd.setColorName("红色");
        return useCase.create(cmd);
    }

    private Factory createFactory() {
        Factory factory = new Factory();
        factory.setFactoryCode("F-PTEST-" + System.nanoTime());
        factory.setFactoryName("测试工厂");
        factory.setProvince("浙江省");
        factory.setCity("杭州市");
        factory.setCooperationStatus(CooperationStatus.ACTIVE);
        return factoryRepository.save(factory);
    }

    // ===== CRUD =====

    @Test
    void create_generatesId() {
        Long id = createProduct();

        assertThat(id).isNotNull();
        ProductPageQuery dto = useCase.getById(id);
        assertThat(dto.getMasterCode()).isNotNull();
        assertThat(dto.getCategory()).isEqualTo(ProductCategory.ORDINARY);
    }

    @Test
    void create_duplicateMasterCode_throws() {
        String code = "DUP" + System.nanoTime();
        createProductWithCode(code);
        createProductWithCode(code);

        assertThatThrownBy(() -> createProductWithCode(code))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    private Long createProductWithCode(String code) {
        ProductCreateCmd cmd = new ProductCreateCmd();
        cmd.setMasterCode(code);
        cmd.setNameZh("商品-" + code);
        return useCase.create(cmd);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> useCase.getById(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void update_changesFields() {
        Long id = createProduct();

        ProductUpdateCmd cmd = new ProductUpdateCmd();
        cmd.setNameZh("修改后名称");

        useCase.update(id, cmd);

        ProductPageQuery dto = useCase.getById(id);
        assertThat(dto.getNameZh()).isEqualTo("修改后名称");
    }

    @Test
    void delete_succeeds() {
        Long id = createProduct();
        useCase.delete(id);

        assertThatThrownBy(() -> useCase.getById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_notFound_throws() {
        assertThatThrownBy(() -> useCase.delete(99999L))
                .isInstanceOf(BusinessException.class);
    }

    // ===== getByMasterCode =====

    @Test
    void getByMasterCode_findsMasterLevel() {
        String code = "GM" + System.nanoTime();
        createProductWithCode(code);

        ProductPageQuery dto = useCase.getByMasterCode(code);
        assertThat(dto.getMasterCode()).isEqualTo(code);
        assertThat(dto.getSubCode()).isNull();
    }

    @Test
    void getByMasterCode_fallsBackToSubProduct() {
        String masterCode = "GMS" + System.nanoTime();
        createProductWithCode(masterCode); // master
        Long subId = createSubProduct(masterCode); // sub

        ProductPageQuery dto = useCase.getByMasterCode(masterCode);
        // 返回 master 或 fallback 到第一条 sub
        assertThat(dto.getMasterCode()).isEqualTo(masterCode);
    }

    @Test
    void getByMasterCode_notFound_throws() {
        assertThatThrownBy(() -> useCase.getByMasterCode("NONEXISTENT-CODE-99999"))
                .isInstanceOf(BusinessException.class);
    }

    // ===== pageQuery =====

    @Test
    void pageQuery_byMasterCode() {
        String code = "GPG" + System.nanoTime();
        createProductWithCode(code);

        ProductQuery query = new ProductQuery();
        query.setMasterCode(code);
        query.setPage(0);
        query.setPageSize(10);

        assertThat(useCase.pageQuery(query).getContent()).hasSize(1);
    }

    @Test
    void pageQuery_byKeyword() {
        String uniqueName = "关键词商品-" + System.nanoTime();
        ProductCreateCmd cmd = new ProductCreateCmd();
        cmd.setMasterCode("KW" + System.nanoTime());
        cmd.setNameZh(uniqueName);
        useCase.create(cmd);

        ProductQuery query = new ProductQuery();
        query.setKeyword("关键词商品");
        query.setPage(0);
        query.setPageSize(10);

        assertThat(useCase.pageQuery(query).getContent()).isNotEmpty();
    }

    @Test
    void pageQuery_pagination() {
        for (int i = 0; i < 3; i++) {
            createProduct();
        }

        ProductQuery query = new ProductQuery();
        query.setPage(0);
        query.setPageSize(2);

        assertThat(useCase.pageQuery(query).getContent()).hasSize(2);
        assertThat(useCase.pageQuery(query).getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    // ===== suggestMasterCodes =====

    @Test
    void suggestMasterCodes_returnsMatching() {
        String prefix = "SGM" + System.nanoTime();
        createProductWithCode(prefix);

        var suggestions = useCase.suggestMasterCodes(prefix);
        assertThat(suggestions).isNotEmpty();
    }

    @Test
    void suggestMasterCodes_emptyKeyword_returnsEmpty() {
        var suggestions = useCase.suggestMasterCodes("");
        assertThat(suggestions).isEmpty();
    }

    @Test
    void suggestMasterCodes_nullKeyword_returnsEmpty() {
        var suggestions = useCase.suggestMasterCodes(null);
        assertThat(suggestions).isEmpty();
    }

    // ===== getProductFactories =====

    @Test
    void getProductFactories_noFactory_returnsEmpty() {
        Long productId = createProduct();

        var factories = useCase.getProductFactories(productId);
        assertThat(factories).isEmpty();
    }

    @Test
    void getProductFactories_withFactory_returnsDetails() {
        Long productId = createProduct();
        Factory factory = createFactory();

        // 手动插入关联
        var pf = new com.manpou.allinone.product.domain.model.ProductFactory();
        pf.setProductId(productId);
        pf.setFactoryId(factory.getId());
        pf.setSupplierSku("SKU-" + System.nanoTime());
        pf.setMoq(100);
        productFactoryJpaRepository.save(pf);

        var factories = useCase.getProductFactories(productId);
        assertThat(factories).hasSize(1);
        assertThat(factories.get(0).getFactoryCode()).isEqualTo(factory.getFactoryCode());
        assertThat(factories.get(0).getFactoryName()).isEqualTo(factory.getFactoryName());
        assertThat(factories.get(0).getMoq()).isEqualTo(100);
    }
}
