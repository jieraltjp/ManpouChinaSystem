package com.manpou.allinone.dispatch.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.dispatch.application.assembler.DispatchAssembler;
import com.manpou.allinone.dispatch.application.dto.DispatchCreateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchQuery;
import com.manpou.allinone.dispatch.application.dto.DispatchUpdateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchVO;
import com.manpou.allinone.dispatch.domain.model.Dispatch;
import com.manpou.allinone.dispatch.domain.repository.DispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchUseCase {

    private final DispatchRepository dispatchRepository;
    private final DispatchAssembler assembler;

    @Transactional(readOnly = true)
    public Page<DispatchVO> pageQuery(DispatchQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(), Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Dispatch> page;
        if (query.getCode() != null && !query.getCode().isBlank()) {
            page = dispatchRepository.findByCodeContainingAndDeletedIsFalse(query.getCode().trim(), pageRequest);
        } else if (query.getDestination() != null && !query.getDestination().isBlank()) {
            page = dispatchRepository.findByDestinationContainingAndDeletedIsFalse(query.getDestination().trim(), pageRequest);
        } else if (query.getManager() != null && !query.getManager().isBlank()) {
            page = dispatchRepository.findByManagerContainingAndDeletedIsFalse(query.getManager().trim(), pageRequest);
        } else {
            page = dispatchRepository.findAllByDeletedIsFalse(pageRequest);
        }

        return page.map(assembler::toVo);
    }

    @Transactional(readOnly = true)
    public DispatchVO getById(Long id) {
        Dispatch entity = dispatchRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new BusinessException("dispatch.not_found", "货物发送记录不存在"));
        return assembler.toVo(entity);
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
}