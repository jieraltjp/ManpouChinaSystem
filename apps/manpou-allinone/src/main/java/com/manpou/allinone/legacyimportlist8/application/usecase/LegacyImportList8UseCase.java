package com.manpou.allinone.legacyimportlist8.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import com.manpou.allinone.legacyimportlist8.application.assembler.LegacyImportList8Assembler;
import com.manpou.allinone.legacyimportlist8.application.dto.CustomsQueryResultVO;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8Query;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8UpdateCmd;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8VO;
import com.manpou.allinone.legacyimportlist8.domain.model.LegacyImportList8;
import com.manpou.allinone.legacyimportlist8.domain.repository.LegacyImportList8Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyImportList8UseCase {

    private final LegacyImportList8Repository repository;
    private final LegacyImportList8Assembler assembler;

    @Transactional(readOnly = true)
    public Page<LegacyImportList8VO> pageQuery(LegacyImportList8Query query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage() != null ? query.getPage() : 0,
                Math.min(query.getPageSize() != null ? query.getPageSize() : 20, 100),
                Sort.by(Sort.Direction.DESC, "updatetime")
        );

        String code = query.getCode();
        String location = query.getLocation();
        String souko = query.getSouko();
        String destination = query.getDestination();

        Specification<LegacyImportList8> spec = Specification.where(null);
        if (code != null && !code.isBlank()) {
            spec = spec.and((root, query2, cb) -> cb.like(root.get("code"), "%" + code + "%"));
        }
        if (location != null && !location.isBlank()) {
            spec = spec.and((root, query2, cb) -> cb.like(root.get("location"), "%" + location + "%"));
        }
        if (souko != null && !souko.isBlank()) {
            spec = spec.and((root, query2, cb) -> cb.like(root.get("souko"), "%" + souko + "%"));
        }
        if (destination != null && !destination.isBlank()) {
            spec = spec.and((root, query2, cb) -> cb.like(root.get("destination"), "%" + destination + "%"));
        }

        Page<LegacyImportList8> page = repository.findAll(spec, pageRequest);
        return page.map(assembler::toDto);
    }

    @Transactional(readOnly = true)
    public LegacyImportList8VO getById(Integer id) {
        LegacyImportList8 entity = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LegacyImportList8", id));
        return assembler.toDto(entity);
    }

    @Transactional
    public LegacyImportList8VO create(LegacyImportList8UpdateCmd cmd) {
        LegacyImportList8 entity = new LegacyImportList8();
        assembler.applyUpdate(entity, cmd);
        entity.setUpdatetime(LocalDateTime.now());
        entity.setUpdateuser(JwtContextHolder.getUsername());
        LegacyImportList8 saved = repository.save(entity);
        log.info("LegacyImportList8 新建: id={}", saved.getId());
        return assembler.toDto(saved);
    }

    @Transactional
    public LegacyImportList8VO update(Integer id, LegacyImportList8UpdateCmd cmd) {
        LegacyImportList8 entity = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LegacyImportList8", id));
        assembler.applyUpdate(entity, cmd);
        entity.setUpdatetime(LocalDateTime.now());
        entity.setUpdateuser(JwtContextHolder.getUsername());
        LegacyImportList8 saved = repository.save(entity);
        log.info("LegacyImportList8 更新: id={}", id);
        return assembler.toDto(saved);
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw BusinessException.notFound("LegacyImportList8", id);
        }
        repository.deleteById(id);
        log.info("LegacyImportList8 删除: id={}", id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    /**
     * 报关批量查询：根据货号列表精准查询（TRIM+UPPER）。
     * 未找到的货号返回 found=false（不抛异常）。
     */
    @Transactional(readOnly = true)
    public List<CustomsQueryResultVO> customsQuery(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        // 去重 + 脱空格
        List<String> trimmed = codes.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        // 逐条精准查询（TRIM+UPPER）
        return trimmed.stream()
                .map(code -> {
                    LegacyImportList8 e = repository.findByCodeTrimUpper(code).orElse(null);
                    if (e == null) {
                        return CustomsQueryResultVO.builder().code(code).found(false).build();
                    }
                    return CustomsQueryResultVO.builder()
                            .code(e.getCode())
                            .found(true)
                            .tax(e.getTax())
                            .unitCh(e.getUnitCh())
                            .rate(e.getRate())
                            .souko(e.getSouko())
                            .location(e.getLocation())
                            .build();
                })
                .toList();
    }
}
