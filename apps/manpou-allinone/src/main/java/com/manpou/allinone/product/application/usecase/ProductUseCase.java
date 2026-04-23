package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;

import java.util.List;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.product.application.assembler.ProductAssembler;
import com.manpou.allinone.product.application.dto.MasterCodeSuggestVO;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.dto.SubCodeSuggestVO;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductJpaRepository;
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
    private final ProductAssembler productAssembler;

    public ProductUseCase(
            @Qualifier("productJpaRepository") ProductRepository productRepository,
            ProductJpaRepository productJpaRepository,
            ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.productJpaRepository = productJpaRepository;
        this.productAssembler = productAssembler;
    }

    /**
     * 分页查询。
     * 支持：按主货号精确、按关键词模糊、按 HS编码精确。
     */
    @Transactional(readOnly = true)
    public Page<ProductPageQuery> pageQuery(ProductQuery query) {
        int pageIndex = query.getPage() == null ? 0 : Math.max(0, query.getPage());
        PageRequest pageRequest = PageRequest.of(
                pageIndex,
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Product> page;
        if (query.getMasterCode() != null && !query.getMasterCode().isBlank()) {
            page = productRepository.findByMasterCodeAndDeletedIsFalse(query.getMasterCode(), pageRequest);
        } else if (query.getHsCode() != null && !query.getHsCode().isBlank()) {
            page = productRepository.findByHsCodeAndDeletedIsFalse(query.getHsCode(), pageRequest);
        } else if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            page = productRepository.findByNameZhContainingAndDeletedIsFalse(query.getKeyword(), pageRequest);
        } else {
            page = productRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(productAssembler::toDto);
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
     * 根据主货号查询（用于步骤1商品选择器自动代入）。
     */
    @Transactional(readOnly = true)
    public ProductPageQuery getByMasterCode(String masterCode) {
        Product entity = productRepository.findByMasterCodeAndDeletedIsFalse(masterCode)
                .orElseThrow(() -> BusinessException.notFound("Product", masterCode));
        return productAssembler.toDto(entity);
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
}
