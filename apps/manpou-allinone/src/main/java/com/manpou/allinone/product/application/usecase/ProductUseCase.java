package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.assembler.ProductAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.product.domain.model.ProductExample;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 示例用例服务。
 * 负责编排业务操作，不含领域逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUseCase {

    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<ProductPageQuery> pageQuery(ProductQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<ProductExample> page = productRepository.findAllByIsDeletedFalse(pageRequest);
        return page.map(productAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ProductPageQuery getById(Long id) {
        ProductExample entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ProductExample", id));
        return productAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(ProductCreateCmd cmd) {
        ProductExample entity = productAssembler.toEntity(cmd);
        ProductExample saved = productRepository.save(entity);
        log.info("[Product] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, ProductUpdateCmd cmd) {
        ProductExample entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ProductExample", id));
        productAssembler.copyToEntity(cmd, entity);
        productRepository.save(entity);
        log.info("[Product] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        ProductExample entity = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("ProductExample", id));
        entity.markDeleted();
        productRepository.save(entity);
        log.info("[Product] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
