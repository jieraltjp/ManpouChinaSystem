package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.factory.application.assembler.FactoryAssembler;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.product.application.assembler.ProductAssembler;
import com.manpou.allinone.product.application.dto.MasterCodeSuggestVO;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductFactoryVO;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.dto.SubCodeSuggestVO;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.model.ProductFactory;
import com.manpou.allinone.product.domain.repository.ProductFactoryRepository;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUseCase {

    private final ProductRepository productRepository;
    private final ProductFactoryRepository productFactoryRepository;
    private final FactoryRepository factoryRepository;
    private final ProductAssembler productAssembler;
    private final FactoryAssembler factoryAssembler;

    @Transactional(readOnly = true)
    public Page<ProductPageQuery> pageQuery(ProductQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Product> page;
        if (query.getMasterCode() != null && !query.getMasterCode().isBlank()) {
            page = productRepository.findByMasterCodeAndIsDeletedFalse(query.getMasterCode(), pageRequest);
        } else if (query.getHsCode() != null && !query.getHsCode().isBlank()) {
            page = productRepository.findByHsCodeAndIsDeletedFalse(query.getHsCode(), pageRequest);
        } else if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            page = productRepository.findByNameZhContainingAndIsDeletedFalse(query.getKeyword(), pageRequest);
        } else {
            page = productRepository.findAll(pageRequest);
        }
        return page.map(productAssembler::toDto);
    }

    @Transactional(readOnly = true)
    public ProductPageQuery getById(Long id) {
        Product entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        return productAssembler.toDto(entity);
    }

    @Transactional(readOnly = true)
    public ProductPageQuery getByMasterCode(String masterCode) {
        Product entity = productRepository.findByMasterCodeAndIsDeletedFalse(masterCode)
                .orElseThrow(() -> BusinessException.notFound("Product", masterCode));
        return productAssembler.toDto(entity);
    }

    @Transactional
    public Long create(ProductCreateCmd cmd) {
        productRepository.findByMasterCodeAndSubCodeAndIsDeletedFalse(
                cmd.getMasterCode(), cmd.getSubCode()
        ).ifPresent(existing -> {
            throw BusinessException.conflict(
                    "product.duplicate_master_code",
                    String.format("货号 %s 已存在", cmd.getMasterCode()));
        });
        Product entity = productAssembler.toEntity(cmd);
        Product saved = productRepository.save(entity);
        log.info("[Product] created, traceId={}, id={}, masterCode={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId(), saved.getMasterCode());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, ProductUpdateCmd cmd) {
        Product entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        productAssembler.copyToEntity(cmd, entity);
        productRepository.save(entity);
        log.info("[Product] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional
    public void delete(Long id) {
        Product entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Product", id));
        entity.markDeleted();
        productRepository.save(entity);
        log.info("[Product] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    @Transactional(readOnly = true)
    public List<MasterCodeSuggestVO> suggestMasterCodes(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productRepository.findMasterCodeSuggestions(keyword.trim()).stream()
                .map(row -> MasterCodeSuggestVO.builder()
                        .masterCode((String) row[0])
                        .nameZh((String) row[1])
                        .colorCount(row[2] == null ? 0 : ((Number) row[2]).intValue())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubCodeSuggestVO> suggestSubCodes(String masterCode) {
        if (masterCode == null || masterCode.isBlank()) {
            return List.of();
        }
        return productRepository.findSubCodesByMasterCode(masterCode.trim()).stream()
                .map(row -> SubCodeSuggestVO.builder()
                        .subCode((String) row[0])
                        .colorName((String) row[1])
                        .build())
                .toList();
    }

    /**
     * 查询商品关联的工厂列表（用于详情抽屉展示）。
     */
    @Transactional(readOnly = true)
    public List<ProductFactoryVO> getProductFactories(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("Product", productId);
        }
        List<ProductFactory> pfs = productFactoryRepository.findByProductId(productId);
        if (pfs.isEmpty()) {
            return List.of();
        }
        List<Long> factoryIds = pfs.stream().map(ProductFactory::getFactoryId).toList();
        List<Factory> factories = factoryRepository.findByIdInAndIsDeletedFalse(factoryIds);
        Map<Long, Factory> factoryMap = factories.stream()
                .collect(Collectors.toMap(Factory::getId, f -> f));
        return pfs.stream().map(pf -> {
            Factory factory = factoryMap.get(pf.getFactoryId());
            return ProductFactoryVO.builder()
                    .productId(pf.getProductId())
                    .factoryId(pf.getFactoryId())
                    .supplierSku(pf.getSupplierSku())
                    .moq(pf.getMoq())
                    .leadTimeDays(pf.getLeadTimeDays())
                    .unitPriceRmb(pf.getUnitPriceRmb())
                    .isPreferred(pf.getIsPreferred())
                    .factoryCode(factory != null ? factory.getFactoryCode() : null)
                    .factoryName(factory != null ? factory.getFactoryName() : null)
                    .province(factory != null ? factory.getProvince() : null)
                    .city(factory != null ? factory.getCity() : null)
                    .contactName(factory != null ? factory.getContactName() : null)
                    .contactPhone(factory != null ? factory.getContactPhone() : null)
                    .cooperationStatus(factory != null ? factory.getCooperationStatus() : null)
                    .build();
        }).toList();
    }
}
