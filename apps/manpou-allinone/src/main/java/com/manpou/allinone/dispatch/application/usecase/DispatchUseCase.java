package com.manpou.allinone.dispatch.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.dispatch.application.assembler.DispatchAssembler;
import com.manpou.allinone.dispatch.application.dto.DispatchCreateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchQuery;
import com.manpou.allinone.dispatch.application.dto.DispatchUpdateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchVO;
import com.manpou.allinone.dispatch.domain.model.Dispatch;
import com.manpou.allinone.dispatch.domain.repository.DispatchRepository;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchUseCase {

    private final DispatchRepository dispatchRepository;
    private final ProductRepository productRepository;
    private final DispatchAssembler assembler;

    @Transactional(readOnly = true)
    public Page<DispatchVO> pageQuery(DispatchQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        Specification<Dispatch> spec = buildSpec(query);
        Page<Dispatch> page = dispatchRepository.findAll(spec, pageRequest);
        return toVoPage(page);
    }

    @Transactional(readOnly = true)
    public List<DispatchVO> exportAll(DispatchQuery query) {
        Specification<Dispatch> spec = buildSpec(query);
        List<Dispatch> all = dispatchRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "id"));
        List<String> codes = all.stream()
                .map(Dispatch::getCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        Map<String, String> nameZhMap = fetchNameZhMap(codes);
        return all.stream()
                .map(entity -> assembler.toVo(entity, nameZhMap.get(entity.getCode())))
                .toList();
    }

    private Specification<Dispatch> buildSpec(DispatchQuery query) {
        Specification<Dispatch> spec = Specification.where(
                (root, q, cb) -> cb.equal(root.get("deleted"), false));
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            // 货号精确前缀匹配（最常用）
            spec = spec.and((root, q, cb) -> cb.like(root.get("code"), kw + "%"));
            // 商品名模糊匹配（跨 product 表）
            List<String> nameZhCodes = productRepository
                    .findByNameZhContainingAndDeletedIsFalse(kw,
                            org.springframework.data.domain.PageRequest.of(0, 50))
                    .getContent().stream()
                    .map(Product::getMasterCode)
                    .filter(c -> c != null)
                    .distinct()
                    .toList();
            if (!nameZhCodes.isEmpty()) {
                spec = spec.and((root, q, cb) -> cb.or(
                        root.get("code").in(nameZhCodes),
                        cb.like(root.get("code"), "%" + kw + "%")
                ));
            } else {
                spec = spec.and((root, q, cb) -> cb.like(root.get("code"), "%" + kw + "%"));
            }
        }
        if (query.getDestManager() != null && !query.getDestManager().isBlank()) {
            String dm = "%" + query.getDestManager().trim() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(root.get("destination"), dm),
                    cb.like(root.get("manager"), dm)
            ));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            String s = query.getStatus().trim();
            if ("incomplete".equals(s)) {
                spec = spec.and((root, q, cb) -> {
                    var empty = cb.equal(root.get("status"), "");
                    var nullStatus = cb.isNull(root.get("status"));
                    return cb.or(empty, nullStatus);
                });
            } else {
                spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), "完成"));
            }
        }
        if (query.getDateFrom() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.get("dispatchDate"), query.getDateFrom()));
        }
        if (query.getDateTo() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.lessThanOrEqualTo(root.get("dispatchDate"), query.getDateTo()));
        }
        return spec;
    }

    private Page<DispatchVO> toVoPage(Page<Dispatch> page) {
        List<String> codes = page.getContent().stream()
                .map(Dispatch::getCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        Map<String, String> nameZhMap = fetchNameZhMap(codes);
        return page.map(entity -> assembler.toVo(entity, nameZhMap.get(entity.getCode())));
    }

    @Transactional(readOnly = true)
    public DispatchVO getById(Long id) {
        Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("dispatch.not_found", "货物发送记录不存在"));
        String productNameZh = null;
        if (entity.getCode() != null && !entity.getCode().isBlank()) {
            productNameZh = productRepository.findByMasterCodeAndDeletedIsFalse(entity.getCode())
                    .map(Product::getNameZh)
                    .orElse(null);
        }
        return assembler.toVo(entity, productNameZh);
    }

    @Transactional(readOnly = true)
    public DispatchVO getLatestByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return dispatchRepository.findTopByCodeAndDeletedIsFalseOrderByIdDesc(code.trim())
                .map(entity -> {
                    String productNameZh = null;
                    if (entity.getCode() != null && !entity.getCode().isBlank()) {
                        productNameZh = productRepository.findByMasterCodeAndDeletedIsFalse(entity.getCode())
                                .map(Product::getNameZh)
                                .orElse(null);
                    }
                    return assembler.toVo(entity, productNameZh);
                })
                .orElse(null);
    }

    private Map<String, String> fetchNameZhMap(List<String> codes) {
        if (codes.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productRepository.findAllByMasterCodeInAndDeletedIsFalse(codes);
        return products.stream()
                .filter(p -> p.getNameZh() != null && !p.getNameZh().isBlank())
                .collect(Collectors.toMap(Product::getMasterCode, Product::getNameZh, (a, b) -> a));
    }

    @Transactional
    public Long create(DispatchCreateCmd cmd) {
        Dispatch entity = assembler.toEntity(cmd);
        Dispatch saved = dispatchRepository.save(entity);
        log.info("[Dispatch] created, id={}, code={}, destination={}",
                saved.getId(), saved.getCode(), saved.getDestination());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, DispatchUpdateCmd cmd) {
        Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("dispatch.not_found", "货物发送记录不存在"));
        assembler.copyUpdate(cmd, entity);
        dispatchRepository.save(entity);
        log.info("[Dispatch] updated, id={}, code={}", id, entity.getCode());
    }

    @Transactional
    public void delete(Long id) {
        Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("dispatch.not_found", "货物发送记录不存在"));
        entity.markDeleted();
        dispatchRepository.save(entity);
        log.info("[Dispatch] deleted, id={}", id);
    }

    @Transactional
    public void patchStatus(Long id, String status) {
        Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("dispatch.not_found", "货物发送记录不存在"));
        entity.setStatus(status != null ? status : "");
        dispatchRepository.save(entity);
        log.info("[Dispatch] patchStatus, id={}, status={}", id, status);
    }

    @Transactional
    public int patchBatchStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (Long id : ids) {
            try {
                Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id).orElse(null);
                if (entity != null) {
                    entity.setStatus(status != null ? status : "");
                    dispatchRepository.save(entity);
                    count++;
                }
            } catch (Exception e) {
                log.warn("[Dispatch] patchBatchStatus failed, id={}: {}", id, e.getMessage());
            }
        }
        log.info("[Dispatch] patchBatchStatus, ids={}, status={}, updated={}", ids.size(), status, count);
        return count;
    }
}