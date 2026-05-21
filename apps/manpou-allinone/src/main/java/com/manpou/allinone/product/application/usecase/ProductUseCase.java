package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;

import java.util.List;
import java.util.Map;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.product.application.assembler.ProductAssembler;
import com.manpou.allinone.product.application.dto.MasterCodeSuggestVO;
import com.manpou.allinone.product.application.dto.ProductCategoryVO;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductFactoryVO;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.dto.SubCodeSuggestVO;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.model.ProductCategory;
import com.manpou.allinone.product.domain.model.ProductFactory;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductFactoryJpaRepository;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductJpaRepository;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品用例服务。
 * 负责编排业务操作，不含领域逻辑。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Slf4j
@Service
public class ProductUseCase {

    private final ProductRepository productRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductFactoryJpaRepository productFactoryJpaRepository;
    private final FactoryRepository factoryRepository;
    private final ProductAssembler productAssembler;

    public ProductUseCase(
            @Qualifier("productJpaRepository") ProductRepository productRepository,
            ProductJpaRepository productJpaRepository,
            ProductFactoryJpaRepository productFactoryJpaRepository,
            FactoryRepository factoryRepository,
            ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.productJpaRepository = productJpaRepository;
        this.productFactoryJpaRepository = productFactoryJpaRepository;
        this.factoryRepository = factoryRepository;
        this.productAssembler = productAssembler;
    }

    /**
     * 分页查询。
     * 支持：按主货号精确、按关键词模糊、按中国HS编码精确、按日本HS编码精确、按工厂名称模糊。
     */
    @Transactional(readOnly = true)
    public Page<ProductPageQuery> pageQuery(ProductQuery query) {
        int pageIndex = query.getPage() == null ? 0 : Math.max(0, query.getPage());
        PageRequest pageRequest = PageRequest.of(
                pageIndex,
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.ASC, "createTime")
        );
        Page<Product> page;
        if (query.getMasterCode() != null && !query.getMasterCode().isBlank()) {
            page = productRepository.findByMasterCodeAndDeletedIsFalse(query.getMasterCode(), pageRequest);
        } else if (query.getHsCode() != null && !query.getHsCode().isBlank()) {
            page = productRepository.findByHsCodeAndDeletedIsFalse(query.getHsCode(), pageRequest);
        } else if (query.getHsCodeJp() != null && !query.getHsCodeJp().isBlank()) {
            page = productRepository.findByHsCodeJpAndDeletedIsFalse(query.getHsCodeJp(), pageRequest);
        } else if (query.getFactoryName() != null && !query.getFactoryName().isBlank()) {
            page = productJpaRepository.findByFactoryNameContaining(query.getFactoryName(), pageRequest);
        } else if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            page = productRepository.findByNameZhContainingAndDeletedIsFalse(query.getKeyword(), pageRequest);
        } else {
            page = productRepository.findAllByDeletedIsFalse(pageRequest);
        }
        // 批量查询关联工厂数量和名称，避免 N+1
        var productIds = page.getContent().stream().map(Product::getId).toList();
        var factoryCountMap = productFactoryJpaRepository.countFactoriesByProductIds(productIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
        var factoryNamesMap = productFactoryJpaRepository.findFactoryNamesByProductIds(productIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> (String) row[1]
                ));
        return page.map(product -> {
            ProductPageQuery dto = productAssembler.toDto(product);
            dto.setFactoryCount(factoryCountMap.getOrDefault(product.getId(), 0));
            dto.setFactoryNames(factoryNamesMap.get(product.getId()));
            return dto;
        });
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ProductPageQuery getById(Long id) {
        Product entity = productRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        return productAssembler.toDto(entity);
    }

    /**
     * 根据主货号查询商品。
     * 优先返回 master-level（sub_code IS NULL），如果没有则返回第一条子货号记录。
     * 用于步骤1商品选择器和需求页商品分类查询。
     */
    @Transactional(readOnly = true)
    public ProductPageQuery getByMasterCode(String masterCode) {
        // 优先查 master-level
        var master = productRepository.findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse(masterCode);
        if (master.isPresent()) {
            return productAssembler.toDto(master.get());
        }
        // fallback：查所有子货号，返回第一条
        var all = productRepository.findAllByMasterCodeAndDeletedIsFalse(masterCode);
        if (!all.isEmpty()) {
            log.info("[ProductUseCase] getByMasterCode masterCode={}, no master, fallback to first subProduct subCode={}",
                    masterCode, all.get(0).getSubCode());
            return productAssembler.toDto(all.get(0));
        }
        throw BusinessException.notFound("Product", masterCode);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(ProductCreateCmd cmd) {
        // 唯一性校验：masterCode + subCode
        productRepository.findByMasterCodeAndSubCodeAndDeletedIsFalse(
                cmd.getMasterCode(), cmd.getSubCode()
        ).ifPresent(existing -> {
            throw BusinessException.conflict(
                    String.format("货号 %s 已存在", cmd.getMasterCode()));
        });
        Product entity = productAssembler.toEntity(cmd);
        Product saved = productRepository.save(entity);
        log.info("[Product] created, traceId={}, id={}, masterCode={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getMasterCode());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, ProductUpdateCmd cmd) {
        Product entity = productRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        productAssembler.copyToEntity(cmd, entity);
        productRepository.save(entity);
        log.info("[Product] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        Product entity = productRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        entity.markDeleted();
        productRepository.save(entity);
        log.info("[Product] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 主货号自动补全。
     */
    @Transactional(readOnly = true)
    public List<MasterCodeSuggestVO> suggestMasterCodes(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productJpaRepository.findMasterCodeSuggestions(keyword.trim()).stream()
                .map(row -> MasterCodeSuggestVO.builder()
                        .masterCode((String) row[0])
                        .nameZh((String) row[1])
                        .colorCount(row[2] == null ? 0 : ((Number) row[2]).intValue())
                        .build())
                .toList();
    }

    /**
     * 子货号候选项（按主货号过滤）。
     */
    @Transactional(readOnly = true)
    public List<SubCodeSuggestVO> suggestSubCodes(String masterCode) {
        if (masterCode == null || masterCode.isBlank()) {
            return List.of();
        }
        return productJpaRepository.findSubCodesByMasterCode(masterCode.trim()).stream()
                .map(row -> SubCodeSuggestVO.builder()
                        .subCode((String) row[0])
                        .colorName((String) row[1])
                        .build())
                .toList();
    }

    /**
     * 批量获取商品类别（解决前端 DemandPage/ProcurementPage N+1 问题）。
     * 一次 SQL 查询返回所有主货号的类别，避免逐个 GET /code/{masterCode} 调用。
     */
    @Transactional(readOnly = true)
    public List<ProductCategoryVO> batchGetCategories(List<String> masterCodes) {
        if (masterCodes == null || masterCodes.isEmpty()) {
            return List.of();
        }
        var uniqueCodes = masterCodes.stream().distinct().toList();
        var rows = productJpaRepository.findCategoryByMasterCodes(uniqueCodes);
        // 一次流式收集 category + imageUrl（避免 toMap 的 null key 问题）
        java.util.Map<String, ProductCategoryVO> map = rows.stream()
                .filter(row -> row[0] != null)
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> ProductCategoryVO.builder()
                                .masterCode((String) row[0])
                                .category(row[1] == null ? null : ((ProductCategory) row[1]).name())
                                .imageUrl((String) row[2])
                                .build(),
                        (v1, v2) -> v1
                ));
        return uniqueCodes.stream()
                .map(code -> map.getOrDefault(code, ProductCategoryVO.builder().masterCode(code).build()))
                .toList();
    }

    /**
     * 获取商品关联的工厂列表（含工厂详情）。
     * 优化：批量查询工厂，避免 N+1。
     */
    @Transactional(readOnly = true)
    public List<ProductFactoryVO> getProductFactories(Long productId) {
        List<ProductFactory> links = productFactoryJpaRepository.findByProductId(productId);
        if (links.isEmpty()) {
            return List.of();
        }
        // 批量一次查出所有工厂
        var factoryMap = factoryRepository.findAllByIdInAndDeletedIsFalse(
                links.stream().map(ProductFactory::getFactoryId).toList()
        );
        return links.stream().map(link -> {
            var factory = factoryMap.get(link.getFactoryId());
            return ProductFactoryVO.builder()
                    .productId(link.getProductId())
                    .factoryId(link.getFactoryId())
                    .supplierSku(link.getSupplierSku())
                    .moq(link.getMoq())
                    .leadTimeDays(link.getLeadTimeDays())
                    .unitPriceRmb(link.getUnitPriceRmb())
                    .isPreferred(link.getIsPreferred())
                    .factoryCode(factory != null ? factory.getFactoryCode() : null)
                    .factoryName(factory != null ? factory.getFactoryName() : null)
                    .province(factory != null ? factory.getProvince() : null)
                    .city(factory != null ? factory.getCity() : null)
                    .contactName(factory != null ? factory.getContactName() : null)
                    .contactPhone(factory != null ? factory.getContactPhone() : null)
                    .cooperationStatus(factory != null && factory.getCooperationStatus() != null
                            ? factory.getCooperationStatus().name() : null)
                    .build();
        }).toList();
    }
}
