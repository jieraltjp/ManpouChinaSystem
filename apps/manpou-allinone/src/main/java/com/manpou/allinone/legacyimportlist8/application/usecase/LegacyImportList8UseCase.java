package com.manpou.allinone.legacyimportlist8.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import com.manpou.allinone.legacyimportlist8.application.assembler.LegacyImportList8Assembler;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        Page<LegacyImportList8> page;
        if (code != null && !code.isBlank()) {
            page = repository.findByCodeContaining(code, pageRequest);
        } else if (location != null && !location.isBlank()) {
            page = repository.findByLocationContaining(location, pageRequest);
        } else if (souko != null && !souko.isBlank()) {
            page = repository.findBySoukoContaining(souko, pageRequest);
        } else if (destination != null && !destination.isBlank()) {
            page = repository.findByDestinationContaining(destination, pageRequest);
        } else {
            page = repository.findAll(pageRequest);
        }
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
}
